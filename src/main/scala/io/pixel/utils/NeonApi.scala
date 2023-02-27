package io.pixel
package utils

import io.pixel.model.*
import io.pixel.utils.json.*
import os.Path
import spray.json.*
import spray.json.DefaultJsonProtocol.*
import sttp.client3.*
import sttp.model.*

import scala.annotation.tailrec
import scala.util.control.Exception.allCatch
import scala.util.{DynamicVariable, Failure, Success, Try}


class NeonApi(val userName: String, val outputPath: Path, newLayout: Boolean = false) {

	private val wwwUri = "https://www.neonmob.com"
	private val apiUri = s"$wwwUri/api"
	private val napiUri = "https://napi.neonmob.com"

	private val userId: Long = findUserId();

	private val itemFiles = ItemFiles(userId, outputPath, newLayout)

	def fetchUserDetails(): Unit =
		val userUri = uri"$apiUri/users/$userId"
		val response = ApiRequest.sendRequest(userUri)

		response.toSeq
			.map(str => JsonObject(str))
			.tapEach(itemFiles.writeUserMetadata(userName, _))


	def fetchSettList(): Either[String, Vector[Sett]] =
		var results: Either[String, Vector[JsonObject]] = Right(Vector.empty[JsonObject])

		var uri = Option(uri"$apiUri/user/collections/$userId/?last_acquired=desc")

		while uri.isDefined && results.isRight do
			val json = fetchCollectionPage(uri.get)

			results = json
				.flatMap(j => j.getObjectArray("results").toRight("Error")) //FIXME
				.flatMap(a => results.map(r => r ++: a))

			uri = json.map(nextUri).getOrElse(None)

		results map { settJson =>
			settJson
				.map(obj => Sett(obj))
				.tapEach(sett => itemFiles.writeMetadata(sett))
		}


	def fetchPieces(sett: Sett): Either[String, Vector[Piece]] =
		val piecesArray =
			if (itemFiles.hasSettPieces(sett)) then
				Right(itemFiles.loadSettPieces(sett))
			else
				val piecesUri = uri"$napiUri/user/$userId/sett/${sett.id}"
				ApiRequest.sendRequest(piecesUri)
					.map(JsonArray(_))

		piecesArray
			.flatMap(_.value)
			.map(array => array.map(Piece(sett, _)))


	def piecesFinished(sett: Sett, pieces: Vector[Piece]): Unit =
		val piecesVetor = pieces
			.map(p => p.definition.keepFields(Array("id", "name")))

		val jsonArray = JsonObject.toArray(piecesVetor)

		itemFiles.writeSettPieces(sett, jsonArray)


	def createPieceDetail(piece: Piece): PieceDetail =
		itemFiles.loadPieceDetail(piece).getOrElse {
			fetchPieceDetail(piece).flatMap(mergePieceWithDetail(piece, _).toRight("Invalid metadata")) match
				case Left(error) =>
					println(s"## Error fetching data: $error.\nTry again later.")
					sys.exit
				case Right(pieceDetail) =>
					itemFiles.writeMetadata(pieceDetail)
					pieceDetail
			}


	def downloadAsset(asset: Asset): Boolean =
		if itemFiles.assetExists(asset) then
			print(".")
			return false

		val response = ApiRequest.downloadBinary(asset.uri)
		itemFiles.writeAsset(asset, response.body)
		print("+")

		true


	private def findUserId(): Long =
		val collectionUri = uri"$wwwUri/$userName/collection"
		val response = ApiRequest.sendRequest(collectionUri)

		val userId = response match
			case Right(str) => parseUserId(str)
			case Left(_) => None

		userId match
			case Some(userId) =>
				println(s"== Found user $userName with userId $userId")
				userId
			case None =>
				println(s"\n## ERROR userId not found for username $userName")
				scala.sys.exit()


	private def parseUserId(body: String) =
		val pattern = "targetId:\\s*(\\d+),".r

		pattern.findFirstMatchIn(body) match
			case Some(pattMatch) => Option(pattMatch.group(1).toLong)
			case _ => None


	private def fetchCollectionPage(uri: Uri): Either[String, JsonObject] =
		val page =
			"""&page=(\d+)""".r
				.findFirstMatchIn(uri.toString)
				.map(m => m.group(1).toInt)
				.getOrElse(1)

		itemFiles.loadCollectionPage(page)
			.map(json => Right(json))
			.getOrElse {
				val response = ApiRequest.sendRequest(uri)

				response.toSeq
					.map(JsonObject(_))
					.tapEach(itemFiles.writeCollectionMetadata(page, _))
					.map(Right(_))
					.head
			}


	private def nextUri(json: JsonObject): Option[Uri] =
		json.getString("next").map(v => uri"$v")


	private def extractFields(piece: Piece, fields: Array[String]): Map[String, JsonObject] =
		piece.definition.fields.filter((k, _) => fields.contains(k))


	private def fetchPieceDetail(piece: Piece): Either[String, JsonObject] =
		val detailUri = uri"$apiUri/users/$userId/piece/${piece.id}/detail/"

		ApiRequest
			.sendRequest(detailUri)
			.map(JsonObject(_))

	private def mergePieceWithDetail(piece: Piece, detailObj: JsonObject) =
		// The key to where the info is located inside the "refs" object
		// is the 2nd item from the "payload" array
		val detailDef = detailObj.getStringArray("payload")
			.map(array => array(1))
			.flatMap(key => detailObj.fields("refs").getObject(key))

		// To hydrate the details map with info from the piece map
		detailDef
			.map(detailDef => detailDef.setFields(extractFields(piece, Array("own_count", "rarity"))))
			.map(PieceDetail(piece, _))

}

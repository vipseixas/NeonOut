package io.pixel
package utils

import io.pixel.model.*
import io.pixel.utils.json.*
import spray.json.*
import spray.json.DefaultJsonProtocol.*
import sttp.client3.*
import sttp.model.*

import scala.annotation.tailrec
import scala.util.control.Exception.allCatch
import scala.util.{DynamicVariable, Failure, Success, Try}


class NeonApi(val userName: String) {

	private val wwwUri = "https://www.neonmob.com"
	private val apiUri = s"$wwwUri/api"
	private val napiUri = "https://napi.neonmob.com"

	private val userId: Long = findUserId();

	private val itemFiles = ItemFiles(userId)

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
				.flatMap(j => j.getObjectArray("results"))
				.flatMap(a => results.map(r => r ++: a))

			uri = json.map(nextUri).getOrElse(None)

		results map { settJson =>
			settJson
				.map(obj => Sett(obj))
				.tapEach(sett => itemFiles.writeMetadata(sett))
		}


	private def fetchCollectionPage(uri: Uri): Either[String, JsonObject] =
		val page = """&page=(\d+)""".r
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


	def fetchPieces(sett: Sett): Either[String, Vector[Piece]] =
		val piecesArray =
			if (itemFiles.hasSettPieces(sett)) then
				Right(itemFiles.loadSettPieces(sett))
			else
				val piecesUri = uri"$napiUri/user/$userId/sett/${sett.id}"
				val response = ApiRequest.sendRequest(piecesUri)
				response.map(JsonArray(_))

		piecesArray
			.flatMap(_.value)
			.map(array => array.map(Piece(sett, _)))


	def piecesFinished(sett: Sett, pieces: Vector[Piece]): Unit =
		val piecesVetor = pieces
			.map(p => p.definition.keepFields(Array("id", "name")))

		val jsonArray = JsonObject.toArray(piecesVetor)

		itemFiles.writeSettPieces(sett, jsonArray)


	def fetchDetail(piece: Piece): Option[PieceDetail] =
		if itemFiles.hasMetadata(piece) then
			return Some(itemFiles.loadPieceDetail(piece))

		val detailObj = fetchPieceDetail(piece)

		val detailDef = detailObj
			.flatMap(obj => obj.getStringArray("payload").map(array => (obj, array)))
			.map((obj, array) => (obj, array(1)))
			.flatMap((obj, key) => obj.fields("refs").getObject(key).toRight("Key not found"))

		// To hydrate the details map with info from the piece map
		val overlayFields = Array("own_count", "rarity")
		detailDef.map(detailDef => detailDef.setFields(extractFields(piece, overlayFields))) match
			case Left(error) =>
				println(s"\n## ERROR fetching details for $piece: $error")
				None
			case Right(detailDef) =>
				val pieceDetail = PieceDetail(piece, detailDef)
				itemFiles.writeMetadata(pieceDetail)
				Option(pieceDetail)


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


	private def nextUri(json: JsonObject): Option[Uri] =
		json.getString("next").map(v => uri"$v")


	private def extractFields(piece: Piece, fields: Array[String]): Map[String, JsonObject] =
		piece.definition.fields.filter((k, _) => fields.contains(k))


	private def fetchPieceDetail(piece: Piece): Either[String, JsonObject] =
		val detailUri = uri"$apiUri/users/$userId/piece/${piece.id}/detail/"
		val response = ApiRequest.sendRequest(detailUri)

		response.map(JsonObject(_))

}

object ApiRequest {
	private val maxSequentialRequests = 10
	private val requestSleep = 2000
	private var requestCounter: Int = maxSequentialRequests

	private val maxRetries: Int = 5
	private val retryInterval: Int = 10

	private val backend: ThreadLocal[SttpBackend[Identity, Any]] = ThreadLocal.withInitial(() => HttpClientSyncBackend())

	@tailrec
	def sendRequest(uri: Uri, retry: Int = 0): Either[String, String] =
		checkRequestWait()

		allCatch.withTry {
			basicRequest.get(uri).send(backend.get)
		} match
			case Success(response) =>
				if response.code.isSuccess then
					response.body
				else
					Left(s"${response.code}: ${response.statusText}")
			case Failure(e) =>
				print(s"\n## Error fetching API: ${e.getMessage}. ")
				if retry < maxRetries then
					println(s"Retrying in $retryInterval seconds...")
					Thread.sleep(retryInterval * 1000)
					sendRequest(uri, retry + 1)
				else
					println(s"Giving up after $retry retries.")
					Left(e.getMessage)

	def downloadBinary(uri: Uri) =
		// The assets come from cloudfront and so we will not restrict throughput
		basicRequest
			.get(uri)
			.response(asByteArrayAlways)
			.send(backend.get)

	private def checkRequestWait(): Unit =
		requestCounter -= 1

		if requestCounter <= 0 then
			Thread.sleep(requestSleep)
			requestCounter = maxSequentialRequests

}

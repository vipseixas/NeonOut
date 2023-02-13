package io.pixel
package utils

import io.pixel.model.*
import io.pixel.utils.ApiRequest
import io.pixel.utils.json.*
import spray.json.*
import spray.json.DefaultJsonProtocol.*
import sttp.client3.*
import sttp.model.*

import scala.annotation.tailrec
import scala.util.control.Exception.allCatch
import scala.util.{Failure, Success, Try}


class NeonApi(val userName: String) {

	private val wwwUri = "https://www.neonmob.com"
	private val apiUri = s"$wwwUri/api"
	private val napiUri = "https://napi.neonmob.com"

	private val userId: Long = findUserId();

	println(s"=== Found user $userName with userId $userId")

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

		println(f"=== Fetching Setts for user $userId")

		while uri.isDefined && results.isRight do
			val json = fetchCollectionPage(uri.get)

			results = json
				.flatMap(j => j.getObjectArray("results"))
				.flatMap(a => results.map(r => r ++: a))

			uri = json.map(nextUri).getOrElse(None)

		results map { settJson =>
			println(f"=== Found ${settJson.length} Setts for user $userId")
			settJson
				.map(obj => Sett(obj))
				.tapEach(sett => itemFiles.writeMetadata(sett))
		}


	private def fetchCollectionPage(uri: Uri): Either[String, JsonObject] =
		val page = """&page=(\d+)""".r
			.findFirstMatchIn(uri.toString)
			.map(m => m.group(1).toInt)
			.getOrElse(1)

		println(s" == Loading collections page ${page}")

		itemFiles.loadCollectionPage(page)
			.map(json => Right(json))
			.getOrElse {
				println(s" == Fetching from URL ${uri}")

				val response = ApiRequest.sendRequest(uri)

				response.toSeq
					.map(JsonObject(_))
					.tapEach(itemFiles.writeCollectionMetadata(page, _))
					.map(Right(_))
					.head
			}


	def fetchPieces(sett: Sett): Either[String, Vector[Piece]] =
		val piecesUri = uri"$napiUri/user/$userId/sett/${sett.id}"
		val response = ApiRequest.sendRequest(piecesUri)

		return response
			.flatMap(JsonArray(_).value)
			.map(array => array.map(Piece(sett, _)))


	def fetchDetail(piece: Piece): Option[PieceDetail] =
		if itemFiles.hasMetadata(piece) then
			return Some(itemFiles.loadPieceDetail(piece))

		val detailObj = fetchPieceDetail(piece)

		val detailDef = detailObj
			.flatMap(obj => obj.getStringArray("payload").map(array => (obj, array)))
			.map((obj, array) => (obj, array(1)))
			.flatMap((obj, key) => obj.fields("refs").getObject(key).toRight("Key not found"))

		detailDef.map(detailDef => detailDef.setFields(extractFields(piece))) match
			case Left(error) =>
				println(s"Error fetching details for $piece: $error")
				None
			case Right(detailDef) =>
				val pieceDetail = PieceDetail(piece, detailDef)
				itemFiles.writeMetadata(pieceDetail)
				Option(pieceDetail)


	def downloadAsset(asset: Asset): Boolean =
		if itemFiles.assetExists(asset) then
			return false

		val response = ApiRequest.downloadBinary(asset.uri)
		itemFiles.writeAsset(asset, response.body)

		return true


	private def findUserId(): Long =
		val collectionUri = uri"$wwwUri/$userName/collection"
		val response = ApiRequest.sendRequest(collectionUri)

		val userId = response match
			case Right(str) => parseUserId(str)
			case Left(_) => None

		userId match
			case Some(userId) => userId
			case None =>
				println(s"ERROR: userId not found for username $userName")
				scala.sys.exit()


	private def parseUserId(body: String) =
		val pattern = "targetId:\\s*(\\d+),".r

		pattern.findFirstMatchIn(body) match
			case Some(pattMatch) => Option(pattMatch.group(1).toLong)
			case _ => None


	private def nextUri(json: JsonObject): Option[Uri] =
		json.getString("next").map(v => uri"$v")


	private def extractFields(piece: Piece): Map[String, JsonObject] =
		// To hydrate the details map with info from the piece map
		val overlayFields = Array("own_count", "rarity")
		piece.definition.fields.filter((k, _) => overlayFields.contains(k))


	private def fetchPieceDetail(piece: Piece): Either[String, JsonObject] =
		println(f"  = Fetching details for $piece")

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

	private val backend = HttpClientSyncBackend()

	@tailrec
	def sendRequest(uri: Uri, retry: Int = 0): Either[String, String] =
		checkRequestWait()

		allCatch.withTry {
			basicRequest.get(uri).send(backend)
		} match
			case Success(response) =>
				if response.code.isSuccess then
					response.body
				else
					Left(s"${response.code}: ${response.statusText}")
			case Failure(e) =>
				print(s"### Error fetching $uri: ${e.getMessage}. ")
				if retry < maxRetries then
					println(s"Retrying again in $retryInterval seconds...")
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
			.send(backend)

	private def checkRequestWait(): Unit =
		requestCounter -= 1

		if requestCounter <= 0 then
			println("--- Sleeping a little ---")
			Thread.sleep(requestSleep)
			requestCounter = maxSequentialRequests

}

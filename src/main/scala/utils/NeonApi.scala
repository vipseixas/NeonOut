package io.pixel
package utils

import spray.json.DefaultJsonProtocol.*
import spray.json.*
import sttp.client3.*
import sttp.model.*

import model.*
import utils.ApiGuard
import utils.json.*


class NeonApi(val userId: Long) {

	private val backend = HttpClientSyncBackend()

	private val apiUri = "https://www.neonmob.com/api"
	private val napiUri = "https://napi.neonmob.com"

	def fetchSettList(userId: Long): Either[String, Vector[Sett]] =
		var results: Either[String, Vector[JsonObject]] = Right(Vector.empty[JsonObject])

		var uri = Option(uri"$apiUri/user/collections/$userId/?last_acquired=desc")

		println(f"=== Fetching Setts for user $userId")

		while uri.isDefined && results.isRight do
			println(f" == Fetching from URL ${uri.get}")

			ApiGuard.checkRequestWait()
			val response = basicRequest
				.get(uri.get)
				.send(backend)

			val json = response.body
				.map(JsonObject(_))

			results = json
				.flatMap(j => j.getObjectArray("results"))
				.flatMap(a => results.map(r => r ++: a))

			uri = json.map(nextUri).getOrElse(None)

		results map { settJson =>
			println(f"=== Found ${settJson.length} Setts for user $userId")
			settJson
				.map(obj => Sett(obj))
				.tapEach(sett => ItemFiles.writeMetadata(sett))
		}


	def fetchPieces(sett: Sett): Either[String, Vector[Piece]] =
		ApiGuard.checkRequestWait()
		val piecesUri = uri"$napiUri/user/$userId/sett/${sett.id}"
		val response = basicRequest
			.get(piecesUri)
			.send(backend)

		return response.body
			.flatMap(JsonArray(_).value)
			.map(array => array.map(Piece(sett, _)))

	def fetchDetail(piece: Piece): Option[PieceDetail] =
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
				ItemFiles.writeMetadata(pieceDetail)
				Option(pieceDetail)


	private def nextUri(json: JsonObject): Option[Uri] =
		json.getString("next").map(v => uri"$v")


	private def extractFields(piece: Piece): Map[String, JsonObject] =
		// To hydrate the details map with info from the piece map
		val overlayFields = Array("own_count", "rarity")
		piece.definition.fields.filter((k, _) => overlayFields.contains(k))


	private def fetchPieceDetail(piece: Piece): Either[String, JsonObject] =
		println(f"  = Fetching details for $piece")

		ApiGuard.checkRequestWait()
		val detailUri = uri"$apiUri/users/$userId/piece/${piece.id}/detail/"
		val response = basicRequest
			.get(detailUri)
			.send(backend)

		response.body
			.map(JsonObject(_))
}

object NeonApi {
	private val backend = HttpClientSyncBackend()

	def downloadAsset(asset: Asset): Array[Byte] =
		ApiGuard.checkRequestWait()
		val response = basicRequest
			.get(asset.uri)
			.response(asByteArrayAlways)
			.send(backend)

		return response.body
}

object ApiGuard {
	private val maxSequentialRequests = 10
	private val requestSleep = 5000
	private var requestCounter: Int = maxSequentialRequests

	def checkRequestWait(): Unit =
		requestCounter -= 1
		if requestCounter <= 0 then
			println("--- Sleeping a little ---")
			Thread.sleep(requestSleep)
			requestCounter = maxSequentialRequests
}


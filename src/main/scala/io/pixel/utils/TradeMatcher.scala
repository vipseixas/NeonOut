package io.pixel.utils

import io.pixel.model.CardStat
import io.pixel.utils.json.{JsonArray, JsonObject}
import sttp.model.Uri
import sttp.model.Uri.*

case class TradeMatcher(wantedId: Long, offeredId: Long) {
	private val baseUrl = "//www.neonmob.com"

	// Initialize with the first page URI, the following pages are in the responses
	private var ownersUriOpt = Option(uri"$baseUrl/api/pieces/$wantedId/owners/?owned=desc&grade=desc".scheme("https"))
	private var needersUriOpt = Option(uri"$baseUrl/api/pieces/$offeredId/needers/?completion=desc&wishlisted=desc".scheme("https"))

	private var owners: Vector[CardStat] = Vector.empty
	private var needers: Vector[CardStat] = Vector.empty


	def execute(pages: Int = 3): Vector[CardStat] =
		Vector.empty[String]

		fetchAllPages(pages)

		val matches = findMatches()

		if (matches.isEmpty)
			println(s"No matches found for $pages pages")

		matches


	private def fetchAllPages(pages: Int) =
		print(s"Fetching $pages pages for both owners and seekers")
		for (_ <- 0 until pages)
			fetchNextPage()
			print(".")
		println()


	private def fetchNextPage(): Unit =
		for { ownersUri <- ownersUriOpt; needersUri <- needersUriOpt }
			val ownersResponse = ApiRequest.sendRequest(ownersUri).map(JsonObject(_))
			val needersResponse = ApiRequest.sendRequest(needersUri).map(JsonObject(_))

			for { ownersResponse <- ownersResponse; needersResponse <- needersResponse }
				ownersUriOpt = createNextPageUri(ownersResponse)
				needersUriOpt = createNextPageUri(needersResponse)

				val newOwners = ownersResponse.getObjectArray("results")
					.getOrElse(Vector.empty)
					.map(CardStat.apply)

				val newNeeders = needersResponse.getObjectArray("results")
					.getOrElse(Vector.empty)
					.map(CardStat.apply)

				owners ++= newOwners
				needers ++= newNeeders


	private def findMatches(): Vector[CardStat] =
		println(s"Trying matches with ${owners.length} owners and ${needers.length} seekers")

		for {
			owner <- owners
			needer <- needers
			if owner.user.equals(needer.user)
		} yield owner


	private def createNextPageUri(response: JsonObject): Option[Uri] =
		response.getString("next")
			.map(r => baseUrl + r)
			.map(u => uri"$u".scheme("https"))
}

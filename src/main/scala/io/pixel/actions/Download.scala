package io.pixel.actions

import io.pixel.model.*
import io.pixel.utils.*

import scala.collection.parallel.CollectionConverters.*
import scala.io.StdIn

class Download {
	private val username = StdIn.readLine("NeonMob username: ")

	private var outputPath = ItemFiles.defaultPath

	private val pathOption = StdIn.readLine(s"Output path [$outputPath]: ")

	if !pathOption.trim.equals("") then
		outputPath = os.Path(pathOption)

	private val neonApi = NeonApi(username, outputPath)

	println(s"== Loading user details for $username")

	neonApi.fetchUserDetails()

	println(s"== Loading Collections for $username")

	neonApi.fetchSettList() match
		case Right(list) => processSettList(list)
		case Left(error) => println(f"Error: $error")


	def processSettList(settList: Vector[Sett]) =
		println(s"== User $username has ${settList.length} collections")
		settList
			.sortBy(_.name.toLowerCase)
			.foreach(processSett)


	def processSett(sett: Sett): Unit =
		neonApi.fetchPieces(sett) match
			case Left(error) =>
				println(s"\n## Error fetching Pieces for $sett: $error")
			case Right(pieces) =>
				println(s"\n== Collection '${sett.name}' has ${pieces.length} cards. Downloading media files...")

				val fromSett = sett.assets
					.map(neonApi.downloadAsset)
					.count(r => r)

				val fromPieces = pieces.map(processPiece).sum

				// Write a marker to indicate that all piece details are already downloaded
				neonApi.piecesFinished(sett, pieces)


	def processPiece(piece: Piece): Int =
		val pieceDetail = neonApi.createPieceDetail(piece)
		processDetail(pieceDetail)


	def processDetail(detail: PieceDetail): Int =
		detail.assets.par
			.map(asset => neonApi.downloadAsset(asset))
			.count(r => r)

}

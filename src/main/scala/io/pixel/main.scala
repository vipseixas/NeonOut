package io.pixel

import io.pixel.model.{Piece, PieceDetail, Sett}
import io.pixel.utils.{ItemFiles, NeonApi}

import scala.collection.parallel.CollectionConverters.*
import scala.io.StdIn


@main
def main(): Unit =

	val username = StdIn.readLine("NeonMob username: ")

	val neonApi = NeonApi(username)

	println(s"== Loading user details for $username")

	neonApi.fetchUserDetails()

	println(s"== Loading Collections for $username")

	neonApi.fetchSettList() match
		case Right(list) => processSettList(list)
		case Left(error) => println(f"Error: $error")


	def processSettList(settList: Vector[Sett]) =
		println(s"== User $username has ${settList.length} collections")
		settList
			.sortBy(_.name)
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

//				println(s"\n = Downloaded ${fromSett+fromPieces} media files\n")


	def processPiece(piece: Piece): Int =
		neonApi.fetchDetail(piece)
			.map(processDetail)
			.getOrElse(0)


	def processDetail(detail: PieceDetail): Int =
		detail.assets.par
			.map(asset => neonApi.downloadAsset(asset))
			.count(r => r)

package io.pixel

import io.pixel.model.{Piece, PieceDetail, Sett}
import io.pixel.utils.{ItemFiles, NeonApi}

import scala.io.StdIn


@main
def main(): Unit =

	val username = StdIn.readLine("NeonMob username: ")

	val neonApi = NeonApi(username)

	neonApi.fetchUserDetails()

	neonApi.fetchSettList() match
		case Right(settList) => settList
			.sortBy(_.name)
			.foreach(processSett)
		case Left(error) => println(f"Error: $error")


	def processSett(sett: Sett): Unit =
		println(f"=== Processing $sett")

		var loaded = 0
		var skipped = 0

		sett.assets foreach { asset =>
			neonApi.downloadAsset(asset) match
				case true => loaded += 1
				case false => skipped += 1
		}

		println(s" == Downloaded $loaded and skipped $skipped assets for $sett")

		neonApi.fetchPieces(sett) match
			case Left(error) =>
				println(s"Error fetching Pieces for $sett: $error")
			case Right(pieces) =>
				println(s" == Found ${pieces.length} Pieces for $sett")
				pieces.foreach(processPiece)


	def processPiece(piece: Piece): Unit =
		println(f" == Processing $piece")

		val detail = neonApi.fetchDetail(piece)

		for { detail <- detail }
			processDetail(detail)


	def processDetail(detail: PieceDetail): Unit =
		var loaded = 0
		var skipped = 0

		detail.assets foreach { asset =>
			neonApi.downloadAsset(asset) match
				case true => loaded += 1
				case false => skipped += 1
		}

		println(s"  = Downloaded $loaded and skipped $skipped assets for $detail")

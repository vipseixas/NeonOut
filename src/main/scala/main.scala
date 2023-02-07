package io.pixel

import model.{Piece, PieceDetail, Sett}
import utils.{ItemFiles, NeonApi}


@main
def main(userId: Long, settId: Long): Unit =

	println(s"=== Collections directory: ${ItemFiles.basePath}")

	val neonApi = NeonApi(userId)

	neonApi.fetchSettList(userId) match
		case Right(settList) if settId > 0 =>
			settList
				.filter(sett => sett.id == settId)
				.take(1)
				.foreach(processSett)
		case Right(settList) if settId == 0 =>
			settList.foreach(processSett)
		case error =>
			println(f"Error: $error")

	def processSett(sett: Sett): Unit =
		println(f"=== Processing $sett")

		var loaded = 0
		var skipped = 0

		sett.assets foreach { asset =>
			ItemFiles.assetExists(asset) match
				case true =>
//					println(s"  = Skipping asset $asset")
					skipped += 1
				case false =>
//					println(s"  = Downloading asset $asset")
					val assetBytes = NeonApi.downloadAsset(asset)
					ItemFiles.writeAsset(asset, assetBytes)
					loaded += 1
		}

		println(s" == Downloaded $loaded and skipped $skipped assets for $sett")

		neonApi.fetchPieces(sett) match
			case Left(error) =>
				println(s"Error fetching Pieces for $sett: $error")
			case Right(pieces) =>
				println(s" == Found ${pieces.length} Pieces for $sett")
				pieces.foreach(processPiece)


	def processPiece(piece: Piece) =
		println(f" == Processing $piece")

		// The metadata is only written after the details are created
		val detail = ItemFiles.hasMetadata(piece) match
			case true => Option(ItemFiles.loadPieceDetail(piece))
			case false => neonApi.fetchDetail(piece)

		for { detail <- detail }
			processDetail(detail)


	def processDetail(detail: PieceDetail) =
		var loaded = 0
		var skipped = 0

		detail.assets foreach { asset =>
			ItemFiles.assetExists(asset) match
				case true =>
//					println(s"Skipping Asset $asset")
					skipped += 1
				case false =>
//					println(s"Downloading Asset $asset")
					val assetBytes = NeonApi.downloadAsset(asset)
					ItemFiles.writeAsset(asset, assetBytes)
					loaded += 1
		}

		println(s"  = Downloaded $loaded and skipped $skipped assets for $detail")

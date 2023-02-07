package io.pixel
package utils

import os.Path
import sttp.model.Uri

import model.*
import utils.json.*


object ItemFiles {

	private val collectionsSubPath = os.sub / "Collections"
	private val documentsSubPath = os.sub / "Documents"
	private val metadataSubPath = os.sub / "metadata"
	private val imagesSubPath = os.sub / "images"

	private var rootPath: Option[Path] = None

	def assetExists(asset: Asset): Boolean =
		os.exists(assetFilename(asset))

	def hasMetadata(item: Item): Boolean =
		os.exists(metaFilename(item))

	def writeMetadata(item: Item): Unit =
		os.write.over(metaFilename(item), item.definition.toString, createFolders = true)

	def writeAsset(asset: Asset, data: Array[Byte]): Unit =
		val filePath = assetFilename(asset)
		os.write.over(filePath, data, createFolders = true)

	def loadPieceDetail(piece: Piece): PieceDetail =
		val metaStr = os.read(metaFilename(piece))
		return PieceDetail(piece, JsonObject(metaStr))

	def basePath: Path =
		rootPath match
			case Some(path) => path
			case None => findBasePath

	private def itemPath(item: Item): Path = basePath / item.parent.slug

	private def findBasePath: Path =
		rootPath = os.exists(os.home / "Documents") match
			case true => Option(os.home / documentsSubPath / collectionsSubPath)
			case false => Option(os.home / collectionsSubPath)

		return rootPath.get

	private def metaFilename(item: Item): Path =
		itemPath(item) / metadataSubPath / f"${item.prefix}${item.slug}.json"

	private def assetFilename(asset: Asset): Path =
		val item = asset.item
		val extension = asset.extension
		itemPath(item) / imagesSubPath / f"${item.prefix}${item.slug}_${asset.name}.$extension"

}

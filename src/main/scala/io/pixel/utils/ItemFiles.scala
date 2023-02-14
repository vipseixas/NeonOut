package io.pixel
package utils

import io.pixel.model.*
import io.pixel.model.utils.json.*
import os.Path
import sttp.model.Uri


case class ItemFiles(userId: Long, newLayout: Boolean = false) {

	private val collectionsSubPath = os.sub / "Collections"
	private val documentsSubPath = os.sub / "Documents"
	private val metadataSubPath = os.sub / "metadata"
	private val imagesSubPath = os.sub / "images"

	private var rootPath: Option[Path] = None

	println(s"== Collections directory: ${basePath}")


	def assetExists(asset: Asset): Boolean =
		os.exists(itemAssetFilename(asset))


	def hasMetadata(item: Item): Boolean =
		os.exists(itemMetaFilename(item))


	def writeMetadata(item: Item): Unit =
		os.write.over(itemMetaFilename(item), item.definition.toString, createFolders = true)


	def writeAsset(asset: Asset, data: Array[Byte]): Unit =
		val filePath = itemAssetFilename(asset)
		os.write.over(filePath, data, createFolders = true)


	def loadPieceDetail(piece: Piece): PieceDetail =
		val metaStr = os.read(itemMetaFilename(piece))
		return PieceDetail(piece, JsonObject(metaStr))


	def writeCollectionMetadata(page: Int, jsonObject: JsonObject): Unit =
		val collectionMetadata = collectionPageFilename(page)
		os.write.over(collectionMetadata, jsonObject.toString, createFolders = true)


	def loadCollectionPage(page: Int): Option[JsonObject] = {
		val collectionMetadata = collectionPageFilename(page)

		if !os.exists(collectionMetadata) then
			return None

		Some(JsonObject(os.read(collectionMetadata)))
	}


	def writeUserMetadata(userName: String, jsonObject: JsonObject): Unit =
		val userMetadata = userFilename(userName)
		os.write.over(userMetadata, jsonObject.toString, createFolders = true)


	def hasSettPieces(sett: Sett): Boolean =
		val fileName = settPiecesMetadata(sett)
		os.exists(fileName)


	def writeSettPieces(sett: Sett, jsonArray: JsonArray): Unit =
		val fileName = settPiecesMetadata(sett)
		os.write.over(fileName, jsonArray.toString, createFolders = true)


	def loadSettPieces(sett: Sett): JsonArray =
		val fileName = settPiecesMetadata(sett)
		JsonArray(os.read(fileName))


	private def basePath: Path =
		rootPath match
			case Some(path) => path
			case None => findBasePath

	private def itemPath(item: Item): Path = basePath / item.parent.slug

	private def metadataPath: Path = basePath / "#metadata"

	private def findBasePath: Path =
		rootPath = os.exists(os.home / "Documents") match
			case true => Option(os.home / documentsSubPath / collectionsSubPath)
			case false => Option(os.home / collectionsSubPath)

		return rootPath.get


	private def userFilename(userName: String) =
		metadataPath / "#users" / s"user_${userId}_$userName.json"


	private def collectionPageFilename(page: Int) =
		metadataPath / "#collections" / s"collections_${userId}_$page.json"


	private def itemMetaFilename(item: Item): Path =
		if newLayout then
			metadataPath / item.parent.slug / s"${item.prefix}${item.slug}_$userId.json"
		else
			itemPath(item) / metadataSubPath / f"${item.prefix}${item.slug}.json"


	private def settPiecesMetadata(sett: Sett) =
		// It is global, does not have user-related info
		metadataPath / sett.slug / s"${sett.prefix}${sett.slug}_pieces.json"


	private def itemAssetFilename(asset: Asset): Path =
		val item = asset.item
		val extension = asset.extension

		var path = itemPath(item)
		if !newLayout then path = path / imagesSubPath

		path / s"${item.prefix}${item.slug}_${asset.name}.$extension"

}

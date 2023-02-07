package io.pixel
package model

import sttp.client3.*
import sttp.model.*

import utils.json.JsonObject


case class Asset(item: Item, name: String, url: String, width: Int = 0, height: Int = 0) {
	def uri: Uri = uri"${this.url}".scheme("https")

	def extension: String =
		var uriPath = os.sub
		val pathSegments = uri.pathSegments.segments
		for segment <- pathSegments do uriPath = uriPath / segment.v

		return uriPath.ext

	override def toString: String = s"$name $extension for '${item.name}'"
}

object Asset {
	private val pieceAssetsKey = "piece_assets"
	private val settAssetsKey = "sett_assets"

	def assetsFrom(item: Item, limitPerType: Int = 3): Vector[Asset] =
		val imageAssets = imageAssetsFrom(item)
		val videoAssets = videoAssetsFrom(item)
		val previewAssets = previewAssetsFrom(item)

		limitAssets(imageAssets, limitPerType)
			++: limitAssets(videoAssets, limitPerType)
			++: previewAssets // Do not limit previews


	private def imageAssetsFrom(item: Item): Vector[Asset] =
		val imageRoot = item match
			case Sett(settDef) =>
				settDef.getObject(settAssetsKey)
			case PieceDetail(_, detailDef) =>
				detailDef.getObject(pieceAssetsKey)
					.flatMap(_.getObject("image"))
			case _ => None

		imageRoot match
			case None => Vector.empty[Asset]
			case Some(obj) => obj.fields
				.flatMap((size, values) => createImageAsset(item, size, values)).toVector


	private def videoAssetsFrom(item: Item): Vector[Asset] =
		val videoRoot = item match
			case PieceDetail(_, detailDef) =>
				detailDef
					.getObject(pieceAssetsKey)
					.flatMap(_.getObject("video"))
			case _ => None

		videoRoot match
			case None => Vector.empty[Asset]
			case Some(obj) => obj.fields
				.flatMap((size, values) => createVideoAsset(item, size, values)).toVector


	private def previewAssetsFrom(item: Item): Vector[Asset] =
		item match
			case Sett(settDef) =>
				settDef.keys
					.filter(key => key.startsWith("preview_"))
					.flatMap(key => settDef.getString(key).map(url => (key, url)))
					.map((key, url) => Asset(item, key, url))
			case _ => Vector.empty[Asset]


	private def limitAssets(assets: Vector[Asset], limit: Int) =
		assets.sortBy(_.width)
			.reverse
			.take(limit)


	private def createImageAsset(item: Item, size: String, obj: JsonObject): Option[Asset] =
		for {
			url <- obj.getString("url")
			width <- obj.getLong("width")
			height <- obj.getLong("height")
		} yield Asset(item, size, url, width.toInt, height.toInt)


	private def createVideoAsset(item: Item, size: String, obj: JsonObject): Vector[Asset] =
		obj.getObjectArray("sources") match
			case Left(_) => Vector.empty[Asset]
			case Right(sources) => sources flatMap { source =>
				for {
					url <- source.getString("url")
					width <- obj.getLong("width")
					height <- obj.getLong("height")
				} yield Asset(item, size, url, width.toInt, height.toInt)
			}

}
package io.pixel
package model

import io.pixel.utils.json.JsonObject
import io.pixel.utils.{ItemFiles, NeonApi}


case class PieceDetail(piece: Piece, definition: JsonObject) extends Item {

	override def parent: Item = piece.parent

	override def itemType: String = "PieceDetail"

	override def slug: String = piece.slug

}

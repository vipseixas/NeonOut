package io.pixel
package model

import utils.json.JsonObject
import utils.{ItemFiles, NeonApi}


case class PieceDetail(piece: Piece, definition: JsonObject) extends Item {

	override def parent: Item = piece.parent

	override def itemType: String = "PieceDetail"

	override def slug: String = piece.slug

}

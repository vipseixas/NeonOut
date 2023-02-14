package io.pixel
package model

import io.pixel.model.{Item, Sett}
import io.pixel.utils.json.JsonObject


case class Piece(sett: Sett, definition: JsonObject) extends Item {

	override def parent: Item = this.sett

	override def itemType: String = "Piece"

	override def slug: String = name.toLowerCase.replaceAll("\\W", "_")

}

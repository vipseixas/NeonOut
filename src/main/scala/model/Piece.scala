package io.pixel
package model

import model.{Item, Sett}
import utils.json.JsonObject


case class Piece(sett: Sett, definition: JsonObject) extends Item {

	override def parent: Item = this.sett

	override def itemType: String = "Piece"

	override def slug: String = name.toLowerCase.replaceAll("\\W", "_")

}

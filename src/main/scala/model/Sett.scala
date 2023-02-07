package io.pixel
package model

import utils.json.JsonObject
import utils.{ItemFiles, NeonApi}


case class Sett(definition: JsonObject) extends Item {

	override def itemType = "Sett"

	override def prefix: String = "sett_"

	override def slug: String = definition.getString("name_slug").getOrElse("#error")

}
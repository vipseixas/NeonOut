package io.pixel
package model

import io.pixel.utils.json.JsonObject
import io.pixel.utils.{ItemFiles, NeonApi}


case class Sett(definition: JsonObject) extends Item {

	override def itemType = "Sett"

	override def prefix: String = "sett_"

	override def slug: String = definition.getString("name_slug").getOrElse("#error")

}

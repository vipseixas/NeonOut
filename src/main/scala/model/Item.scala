package io.pixel
package model

import io.pixel.utils.json.JsonObject

abstract class Item {

	def definition: JsonObject

	def id: Long = definition.getLong("id").getOrElse(-1)

	def name: String = definition.getString("name").getOrElse("#error").trim

	def prefix: String = ""

	def parent: Item = this

	def itemType: String

	def slug: String

	def assets: Vector[Asset] = Asset.assetsFrom(this)

	override def toString: String = s"$itemType:$id '$name'"

}

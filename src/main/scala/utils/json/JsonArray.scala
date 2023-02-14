package io.pixel
package utils.json

import io.pixel.utils.json.JsonObject
import spray.json.*


class JsonArray(json: String) {

	private val rootObject: JsValue = json.parseJson

	def value: Either[String, Vector[JsonObject]] = rootObject match
		case JsArray(array) => Right(array.map(i => JsonObject(i.toString)))
		case _ => Left("Object is not an array")

	override def toString: String = rootObject.prettyPrint
}

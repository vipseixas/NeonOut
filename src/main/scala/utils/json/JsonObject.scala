package io.pixel
package utils.json

import spray.json.*
import spray.json.DefaultJsonProtocol.*


class JsonObject(json: String) {

	private val rootObject: JsValue = json.parseJson

	def getLong(fieldName: String): Option[Long] =
		objFields.getOrElse(fieldName, JsNull) match
			case JsNumber(num) => Some(num.longValue)
			case _ => None

	def getString(fieldName: String): Option[String] =
		objFields.getOrElse(fieldName, JsNull) match
			case JsString(str) => Some(str)
			case _ => None

	def getObject(fieldName: String): Option[JsonObject] =
		objFields.getOrElse(fieldName, JsNull) match
			case JsObject(obj) => Some(JsonObject(JsObject(obj).toString))
			case _ => None

	def getObjectArray(fieldName: String): Either[String, Vector[JsonObject]] =
		objFields.getOrElse(fieldName, JsNull) match
			case JsArray(array) => Right(array.map(v => JsonObject(v.toString)))
			case _ => Left(s"$fieldName is not an array")

	def getStringArray(fieldName: String): Either[String, Vector[String]] =
		objFields.getOrElse(fieldName, JsNull) match
			case JsArray(array) => Right(array.map(v => v.convertTo[String]))
			case _ => Left(s"$fieldName is not an array")

	def keys: Vector[String] = objFields.keys.toVector

	def fields: Map[String, JsonObject] =
		objFields.map((k, v) => (k, JsonObject(v.toString)))

	def setFields(fields: Map[String, JsonObject]): JsonObject =
		val newFields = asObject
			.map(_.fields ++ fields.map((k, v) => (k, v.rootObject)))
			.getOrElse(Map.empty)
		JsonObject(JsObject(newFields).toString)

	def keepFields(fields: Array[String]): JsonObject =
		val newFields = asObject
			.map(_.fields.filter((k, _) => fields.contains(k)))
			.getOrElse(Map.empty)
		JsonObject(JsObject(newFields).toString)

	override def toString: String = rootObject.prettyPrint

	private def asObject: Option[JsObject] = rootObject match
		case JsObject(_) => Some(rootObject.asJsObject)
		case _ => None

	private def objFields: Map[String, JsValue] =
		asObject
			.map(o => o.fields)
			.getOrElse(Map.empty)
}

object JsonObject {
	def toArray(vector: Vector[JsonObject]): JsonArray =
		val jsArray = JsArray(vector.map(obj => obj.rootObject))
		JsonArray(jsArray.toString)
}

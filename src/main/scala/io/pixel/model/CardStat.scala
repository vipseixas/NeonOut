package io.pixel.model

import io.pixel.utils.json.JsonObject

case class CardStat(definition: JsonObject) {

	def user: String = definition.getString("username").getOrElse("#error").trim

	def name: String = definition.getString("name").getOrElse("#error").trim

	def bio: String = definition.getString("bio").getOrElse("#error").trim

	def traderScore: Long = definition.getLong("trader_score").getOrElse(0L)

	def printCount: Long = definition.getLong("print_count").getOrElse(0L)

	def wishListed: Boolean = definition.getLong("wishlisted").getOrElse(0L) > 0

	def ownedPercentage: Long = definition.getLong("owned_percentage").getOrElse(0L)

	def grade: String = traderScore match
		case x if x < 1 => "F"
		case x if x < 2 => "F+"
		case x if x < 3 => "D-"
		case x if x < 4 => "D"
		case x if x < 5 => "D+"
		case x if x < 6 => "C-"
		case x if x < 7 => "C"
		case x if x < 8 => "C+"
		case x if x < 9 => "B-"
		case x if x < 10 => "B"
		case x if x < 11 => "B+"
		case x if x < 12 => "A-"
		case x if x < 13 => "A"
		case _ => "A+"


	override def toString: String = s"User '$name' ($user) has grade $grade, completed $ownedPercentage% and has $printCount prints (wishlisted? $wishListed)."
}

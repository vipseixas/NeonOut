package io.pixel.actions

import io.pixel.utils.TradeMatcher

class FindMatch {
	val result = TradeMatcher(253345, 253344).execute(3)
	result.foreach(println)
}

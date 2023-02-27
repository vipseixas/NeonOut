package io.pixel

import io.pixel.actions.*
import io.pixel.model.{Piece, PieceDetail, Sett}
import io.pixel.utils.{ItemFiles, NeonApi, TradeMatcher}

import scala.io.StdIn

@main
def main(): Unit =
	print("Choose an action:\n\t1 - Download cards\n\t2 - Find a match\n=> ")

	StdIn.readInt() match
		case 1 => Download()
		case 2 => FindMatch()
		case _ => println("Invalid option")

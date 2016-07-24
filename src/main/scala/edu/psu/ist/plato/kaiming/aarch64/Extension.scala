package edu.psu.ist.plato.kaiming.aarch64

import enumeratum._

sealed trait Extension extends EnumEntry  

object Extension extends Enum[Extension] {
  
  val values = findValues
  
  case object Signed extends Extension 
  case object Unsigned extends Extension 
  case object NoExtension extends Extension
  
}


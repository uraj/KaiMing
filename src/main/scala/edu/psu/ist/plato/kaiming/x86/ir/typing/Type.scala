package edu.psu.ist.plato.kaiming.x86.ir.typing

sealed abstract class Type(code : Int) {
  private val encoding = code
  
  private var mapping = Map[Int, Type]((0, Bot), (1, Int), (2, Ptr), (3, Top))

  private def get(x : Int) : Type = mapping.get(x).orNull
  
  /* Meet */
  def /\(that : Type) = get(encoding & that.encoding)
  
  /* Join */
  def \/(that : Type) = get(encoding | that.encoding)
}

object Bot extends Type(0) {
  override def toString() = "Bot"
}

object Int extends Type(1) {
  override def toString() = "Int"
}

object Ptr extends Type(2) {
  override def toString() = "Ptr"
}

object Top extends Type(3) {
  override def toString() = "Top"
}

class TypeVar(varid : Int) {
  val id = varid
  var upper : Type = Top
  var lower : Type = Bot
  
  def isDetermined = upper == lower
    
  override def equals(that : Any) = that match {
    case that : TypeVar => that.id == id 
    case _ => false
  }
  
  def sameRange(that : TypeVar) = that.upper == upper && that.lower == lower
  
  override def toString() = id + ":" + upper + "->" + lower
}
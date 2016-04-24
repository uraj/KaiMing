package edu.psu.ist.plato.kaiming.x86.ir.typing

sealed abstract class Type(code : Int) {
  val encoding = code
  
  private var mapping = Map[Int, Type]((0, TBot), (1, TInt), (2, TPtr), (3, TTop))

  private def get(x : Int) : Type = mapping.get(x).orNull
  
  /* Meet */
  def /\(that : Type) = get(encoding & that.encoding)
  
  /* Join */
  def \/(that : Type) = get(encoding | that.encoding)
}

object TBot extends Type(0) {
  override def toString() = "Bot"
}

object TInt extends Type(1) {
  override def toString() = "Int"
}

object TPtr extends Type(2) {
  override def toString() = "Ptr"
}

object TTop extends Type(3) {
  override def toString() = "Top"
}

abstract class TypeVar(varid : Int) {
  val id = varid
    
  override def equals(that : Any) = that match {
    case that : TypeVar => that.id == id 
    case _ => false
  }
}

case class RangedTypeVar(varid : Int, upper : Type = TTop, lower : Type = TBot)
  extends TypeVar(varid) {
  def isDetermined = upper == lower
  
  def sameRange(that : RangedTypeVar) = that.upper == upper && that.lower == lower
  
  override def toString() = id + ":" + upper + "->" + lower
}

sealed case class ConstTypeVar(t : Type) extends TypeVar(-t.encoding)

object IntVar extends ConstTypeVar(TInt)
object PtrVar extends ConstTypeVar(TPtr)

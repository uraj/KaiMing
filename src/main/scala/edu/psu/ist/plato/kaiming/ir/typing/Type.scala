package edu.psu.ist.plato.kaiming.ir.typing

sealed abstract class Type {
  /* Meet */
  def /\(that : Type) : Type
  
  /* Join */
  def \/(that : Type) : Type
}

case object TBot extends Type {
  override def toString() = "Bot"
  
  override def /\(that : Type) = this
  override def \/(that : Type) = that
}

case object TInt extends Type {
  override def toString() = "Int"
  
  override def /\(that : Type) : Type = that match {
    case TInt => this
    case TPtr => TBot
    case TTop | TBot => that /\ this 
  }
  
  override def \/(that : Type) : Type = that match {
    case TInt => this
    case TPtr => TTop
    case TTop | TBot => that \/ this 
  }
}

case object TPtr extends Type {
  override def toString() = "Ptr"
  
  override def /\(that : Type) : Type = that match {
    case TPtr => this
    case TInt => TBot
    case TTop | TBot => that /\ this 
  }
  
  override def \/(that : Type) : Type = that match {
    case TPtr => this
    case TInt => TTop
    case TTop | TBot => that \/ this 
  }
}

case object TTop extends Type {
  override def toString() = "Top"
  
  override def /\(that : Type) = that
  override def \/(that : Type) = this
}

abstract class TypeVar(varid : Int) {
  val id = varid
    
  override def equals(that : Any) = that match {
    case that : TypeVar => that.id == id 
    case _ => false
  }
  
  protected var upper_ : Type = TTop
  protected var lower_ : Type = TBot
  
  def setUpper(t : Type) : Boolean
  def setLower(t : Type) : Boolean
  def setUpperLower(t1 : Type, t2 : Type) = { setUpper(t1); setLower(t2) }
  
  def upper = upper_
  def lower = lower_
  
  def isDetermined = upper.equals(lower)
}

class MutableTypeVar(varid : Int) extends TypeVar(varid) {
  require(varid >= 0)
  override def setUpper(t : Type) = { val ret = !upper.equals(t); upper_ = t; ret }
  override def setLower(t : Type) = { val ret = !lower.equals(t); lower_ = t; ret }
  override def toString() = "[" + upper + "->" + lower + "]"
}
 
class ConstTypeVar(varid : Int, t : Type) extends TypeVar(varid) {
  require(varid < 0)
  val ty = t
  upper_ = ty
  lower_ = ty
  override def setUpper(t : Type) = false
  override def setLower(t : Type) = false
  override def toString() = ty.toString()
}
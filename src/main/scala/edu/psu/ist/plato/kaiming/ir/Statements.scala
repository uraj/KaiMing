package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming._

import edu.psu.ist.plato.kaiming.utils.Exception

sealed abstract class Stmt[A <: Arch](val usedExpr: Vector[Expr]) extends Entry[KaiMing] {
  
  final def this(exprs: Expr*) = this(exprs.toVector)
  
  val host: MachEntry[A]
  
  final def usedLvals = usedExpr.map(_.enumLvals).fold(Set[Lval]())(_|_)
  
  override def toString = index.toString
  
}

case class StStmt[A <: Arch](index: Long, host: MachEntry[A],
    storeTo: Expr, storedExpr: Expr) extends Stmt[A](storeTo, storedExpr)

case class JmpStmt[A <: Arch](index: Long, host: MachEntry[A] with Terminator[A],
    cond: Expr, target: Expr) extends Stmt[A](cond, target) with Terminator[KaiMing] {
  
  def dependentLvals = cond.enumLvals
  
  override def isConditional = !dependentLvals.isEmpty
  override def isCall = false
  override def isReturn = false
  override def isIndirect = target.isInstanceOf[Reg]
  override def isTargetConcrete = target.isInstanceOf[Const]
  override def targetIndex = Exception.unsupported()
  
}

case class RetStmt[A <: Arch](index: Long, host: MachEntry[A],
    target: Expr) extends Stmt[A](target) with Terminator[KaiMing] {
  
  override def isConditional = false
  override def isCall = false
  override def isReturn = true
  override def isIndirect = target.isInstanceOf[Reg]
  override def isTargetConcrete = target.isInstanceOf[Const]
  override def targetIndex = Exception.unsupported()
  
}

sealed abstract class DefStmt[A <: Arch](usedExpr: Vector[Expr])
    extends Stmt[A](usedExpr) {
  
  def this(exprs: Expr*) = this(exprs.toVector)
  def definedLval: Lval
  
}

case class AssignStmt[A <: Arch](index: Long, host: MachEntry[A],
    definedLval: Lval, usedRval: Expr) extends DefStmt[A](Vector(usedRval)) {
  
  require(definedLval.sizeInBits == usedRval.sizeInBits)
  
}

sealed trait Extractor extends enumeratum.EnumEntry
object Extractor extends enumeratum.Enum[Extractor] {
  
  val values = findValues

  case object Carry extends Extractor
  case object Overflow extends Extractor
  case object Zero extends Extractor
  case object Negative extends Extractor
  
}

case class CallStmt[A <: Arch](index: Long, host: MachEntry[A] with Terminator[A],
    target: Expr) extends DefStmt[A](target) {
  
  override def definedLval = Reg(host.mach.returnRegister)
  
}

case class SelStmt[A <: Arch](index: Long, host: MachEntry[A],
    definedLval: Lval, condition: Expr, trueValue: Expr, falseValue: Expr)
    extends DefStmt[A](condition, trueValue, falseValue) {
  
  require(definedLval.sizeInBits == trueValue.sizeInBits &&
      definedLval.sizeInBits == falseValue.sizeInBits)
  
}
  
case class LdStmt[A <: Arch](index: Long, host: MachEntry[A],
    definedLval: Lval, loadFrom: Expr) extends DefStmt[A](loadFrom)

case class NopStmt[A <: Arch](index: Long, host: MachEntry[A]) extends Stmt[A]()

case class UnsupportedStmt[A <: Arch](index: Long, host: MachEntry[A]) extends Stmt[A]()

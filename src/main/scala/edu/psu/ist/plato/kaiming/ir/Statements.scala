package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming.Entry
import edu.psu.ist.plato.kaiming.Terminator
import edu.psu.ist.plato.kaiming.MachEntry
import edu.psu.ist.plato.kaiming.BBlock
import edu.psu.ist.plato.kaiming.Arch.KaiMing
import edu.psu.ist.plato.kaiming.MachArch

sealed abstract class Stmt[A <: MachArch](val usedExpr: Vector[Expr]) extends Entry[KaiMing] {
  
  final def this(exprs: Expr*) = this(exprs.toVector)
  
  val host: MachEntry[A]
  
  final def usedLvals = usedExpr.map(_.enumLvals).fold(Set[Lval]())(_|_)
  
  override def toString = index.toString
  
}

case class StStmt[A <: MachArch](index: Long, host: MachEntry[A],
    storeTo: Expr, storedExpr: Expr) extends Stmt[A](storeTo, storedExpr)

object JmpStmt {
  // FIXME: This is not a really good implementation of target relocation,
  // for it hinders garbage collection when a JmpStmt is not longer actually
  // in use. It can be a problem for IR because we plan to support IR
  // transformation 
  import scala.collection.mutable.{Map => MMap}
  private val _relocationTable : MMap[JmpStmt[_ <: MachArch], BBlock[KaiMing]] = MMap()
    
  private def relocate(js: JmpStmt[_ <: MachArch], bb: BBlock[KaiMing]) =
    _relocationTable.put(js, bb)

  private def lookUpRelocation(js: JmpStmt[_ <: MachArch]) = _relocationTable.get(js)

}

case class JmpStmt[A <: MachArch](index: Long, host: MachEntry[A] with Terminator[A],
    cond: Expr, target: Expr) extends Stmt[A](cond, target) {
  
  val dependentLvals = cond.enumLvals
  
  def isConditional = !dependentLvals.isEmpty
  
  def relocate(bb: BBlock[KaiMing]) = JmpStmt.relocate(this, bb)
  def relocatedTarget = JmpStmt.lookUpRelocation(this)
  
}

case class RetStmt[A <: MachArch](index: Long, host: MachEntry[A],
    target: Expr) extends Stmt[A](target)

sealed abstract class DefStmt[A <: MachArch](usedExpr: Vector[Expr])
    extends Stmt[A](usedExpr) {
  
  def this(exprs: Expr*) = this(exprs.toVector)
  def definedLval: Lval
  
}

case class AssignStmt[A <: MachArch](index: Long, host: MachEntry[A],
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

case class CallStmt[A <: MachArch](index: Long, host: MachEntry[A] with Terminator[A],
    target: Expr) extends DefStmt[A](target) {
  
  override def definedLval = Reg(host.mach.returnRegister)
  
}

case class SelStmt[A <: MachArch](index: Long, host: MachEntry[A],
    definedLval: Lval, condition: Expr, trueValue: Expr, falseValue: Expr)
    extends DefStmt[A](condition, trueValue, falseValue) {
  
  require(definedLval.sizeInBits == trueValue.sizeInBits &&
      definedLval.sizeInBits == falseValue.sizeInBits)
  
}
  
case class LdStmt[A <: MachArch](index: Long, host: MachEntry[A],
    definedLval: Lval, loadFrom: Expr) extends DefStmt[A](loadFrom)

case class NopStmt[A <: MachArch](index: Long, host: MachEntry[A]) extends Stmt[A]()

case class UnsupportedStmt[A <: MachArch](index: Long, host: MachEntry[A]) extends Stmt[A]()

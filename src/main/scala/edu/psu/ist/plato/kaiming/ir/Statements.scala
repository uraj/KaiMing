package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming.Entry
import edu.psu.ist.plato.kaiming.Terminator
import edu.psu.ist.plato.kaiming.MachEntry
import edu.psu.ist.plato.kaiming.BBlock
import edu.psu.ist.plato.kaiming.Arch.KaiMing
import edu.psu.ist.plato.kaiming.MachArch

object Stmt {
  
  implicit def toIRStmt(e: Entry[KaiMing]) = e.asInstanceOf[Stmt]
  
}

sealed abstract class Stmt(val usedExpr: Expr*) extends Entry[KaiMing] {
  val host: MachEntry[_ <: MachArch]
}

case class StStmt(override val index: Long, override val host: MachEntry[_ <: MachArch],
    storeTo: Expr, storedExpr: Expr) extends Stmt(storeTo, storedExpr)

object JmpStmt {
  
  // FIXME: This is not a really good implementation of target relocation,
  // for it hinders garbage collection when a JmpStmt is not longer actually
  // in use. It can be a problem for IR because we plan to support IR
  // transformation 
  import scala.collection.mutable.{Map => MMap}
  private val _relocationTable : MMap[JmpStmt, BBlock[KaiMing]] = MMap()
    
  private def relocate(js: JmpStmt, bb: BBlock[KaiMing]) =
    _relocationTable.put(js, bb)

  private def lookUpRelocation(js: JmpStmt) = _relocationTable.get(js)
  
}

case class JmpStmt(override val index: Long,
    override val host: MachEntry[A] with Terminator[A] forSome { type A <: MachArch },
    target: Expr) extends Stmt(target) {
  
  def dependentFlags = host.dependentFlags
  def isConditional = !dependentFlags.isEmpty
  
  def relocate(bb: BBlock[KaiMing]) = JmpStmt.relocate(this, bb)
  def relocatedTarget = JmpStmt.lookUpRelocation(this)
  
}

case class RetStmt(override val index: Long, override val host: MachEntry[_ <: MachArch])
    extends Stmt()

sealed abstract class DefStmt(usedExpr: Expr*) extends Stmt(usedExpr:_*) {
  
  def definedLval: Lval
  
}

case class AssignStmt(override val index: Long, override val host: MachEntry[_ <: MachArch],
    override val definedLval: Lval, usedRval: Expr, range: (Int, Int))
    extends DefStmt(usedRval) {
  
  def isPartial = range._1 != 0 || range._2 != definedLval.sizeInBits
  
}

sealed trait Extractor extends enumeratum.EnumEntry
object Extractor extends enumeratum.Enum[Extractor] {
  
  val values = findValues

  case object Carry extends Extractor
  case object Overflow extends Extractor
  case object Zero extends Extractor
  case object Negative extends Extractor
  
}

case class SetFlgStmt(override val index: Long, override val host: MachEntry[_ <: MachArch],
    extractor: Extractor, override val definedLval: Flg, usedRval: CompoundExpr)
    extends DefStmt(usedRval)

case class CallStmt(override val index: Long,
    override val host: MachEntry[A] with Terminator[A] forSome { type A <: MachArch },
    dependentFlags: Set[Flg], target: Expr) extends DefStmt(target) {
  
  override def definedLval = Reg(host.mach.returnRegister)
  
}

case class SelStmt(override val index: Long, override val host: MachEntry[_ <: MachArch],
    override val definedLval: Lval, condition: Expr, trueValue: Expr, falseValue: Expr)
    extends DefStmt(condition, trueValue, falseValue)
  
case class LdStmt(override val index: Long, override val host: MachEntry[_ <: MachArch],
    override val definedLval: Lval, loadFrom: Expr) extends DefStmt(loadFrom)

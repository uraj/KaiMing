package edu.psu.ist.plato.kaiming

import utils.Indexed

object Entry {
  
  def search[A <: Arch](entries: Seq[Entry[A]], idx: Long) =
    entries.map(x => x.index).indexOf(idx) match {
      case -1 => None
      case i => Some(i)
    }

}

abstract class Entry[A <: Arch] extends Indexed {
  
  override final def hashCode = index.hashCode()
  override final def equals(that: Any) = 
    that.isInstanceOf[AnyRef] && (this eq that.asInstanceOf[AnyRef])
  
}

abstract class MachEntry[A <: MachArch] extends Entry[A] {
  
  val mach: Machine[A]
  
  final def isTerminator: Boolean = this.isInstanceOf[Terminator[_]]
  final def asTerminator = this.asInstanceOf[Terminator[A]]

}

trait Terminator[A <: MachArch] {
  self : MachEntry[A] =>
  def isIndirect: Boolean
  def isReturn: Boolean
  def isCall: Boolean
  final def isInterprocedural: Boolean = isCall || isReturn
  final def isIntraprocedural = !isInterprocedural
  def isTargetConcrete: Boolean
  def isConditional: Boolean
  def targetIndex: Long
  def relocate(target: MachBBlock[A]): Unit
  def relocatedTarget: Option[MachBBlock[A]]
}
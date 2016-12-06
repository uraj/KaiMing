package edu.psu.ist.plato.kaiming

import utils.Indexed

abstract class Entry[A <: Arch] extends Indexed {
  
  override final def hashCode = index.hashCode()
  override final def equals(that: Any) = 
    that.isInstanceOf[AnyRef] && (this eq that.asInstanceOf[AnyRef])
    
  final def isTerminator: Boolean = this.isInstanceOf[Terminator[A]]
  final def asTerminator = this.asInstanceOf[Terminator[A]]
  
}

abstract class MachEntry[A <: MachArch] extends Entry[A] {
  
  def mach: Machine[A]
  
}

trait Terminator[A <: Arch] {
  self : Entry[A] =>
  def isIndirect: Boolean
  def isReturn: Boolean
  def isCall: Boolean
  final def isInterprocedural: Boolean = isCall || isReturn
  final def isIntraprocedural = !isInterprocedural
  def isTargetConcrete: Boolean
  def isConditional: Boolean
  def targetIndex: Long
  
  private var _target: Option[BBlock[A]] = None
    
  final protected[kaiming] def relocate(target: BBlock[A]) = _target = Some(target)
  final def relocatedTarget: Option[BBlock[A]] = _target
}
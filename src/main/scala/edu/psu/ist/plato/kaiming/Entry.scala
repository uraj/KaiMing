package edu.psu.ist.plato.kaiming

object Entry {
  
  def search[A <: Arch](entries: Seq[Entry[A]], idx: Long) =
    entries.map(x => x.index).indexOf(idx)

}

abstract class Entry[A <: Arch] extends Ordered[Entry[A]] {
  
  def index: Long
    
  override def hashCode = index.hashCode()
  override def equals(that: Any) = 
    that.isInstanceOf[AnyRef] && (this eq that.asInstanceOf[AnyRef])
  
  // Ordered
  override final def compare(that: Entry[A]) =
    Math.signum(index - that.index).toInt
  
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
  def dependentFlags: Set[MachFlag[A]]
  final def isConditional = !dependentFlags.isEmpty
  def targetIndex: Long
  def relocate(target: MachBBlock[A]): Unit
  def relocatedTarget: Option[MachBBlock[A]]
}
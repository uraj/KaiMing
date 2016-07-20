package edu.psu.ist.plato.kaiming

object Entry {
  trait Terminator[A <: Arch] {
    def isIndirect: Boolean
    def isReturn: Boolean
    def isCall: Boolean
    final def isInterprocedural: Boolean = isCall || isReturn
    final def isIntraprocedural = !isInterprocedural
    def isTargetConcrete: Boolean
    def isConditional: Boolean
    def targetIndex: Long
    def relocate(target: BasicBlock[A]): Unit
  }
  
  def search[A <: Arch](entries: Seq[Entry[A]], idx: Long) =
    entries.map(x => x.index).indexOf(idx)

}

abstract class Entry[A <: Arch] extends Ordered[Entry[A]] {
  
  def index: Long
  val machine: Machine[A]
  final def isTerminator: Boolean = this.isInstanceOf[Entry.Terminator[_]]
  final def asTerminator = this.asInstanceOf[Entry.Terminator[A]]
  
  override def hashCode = index.hashCode()
  override def equals(that: Any) = 
    that.isInstanceOf[AnyRef] && (this eq that.asInstanceOf[AnyRef])
  
  // Ordered
  override final def compare(that: Entry[A]) =
    Math.signum(index - that.index).toInt
  
}

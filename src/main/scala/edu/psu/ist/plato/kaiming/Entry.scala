package edu.psu.ist.plato.kaiming

object Entry {
  trait Terminator[T <: Entry] {
    def isIndirect: Boolean
    def isReturn: Boolean
    def isCall: Boolean
    final def isInterprocedural: Boolean = isCall || isReturn
    final def isIntraprocedural = !isInterprocedural
    def isTargetConcrete: Boolean
    def isConditional: Boolean
    def targetIndex: Long
  }
  
  def search(entries: Seq[Entry], idx: Long) =
    entries.map(x => x.index).indexOf(idx)

}

abstract class Entry extends Ordered[Entry] {
  
  def index: Long
  val machine: Machine
  final def isTerminator: Boolean = this.isInstanceOf[Entry.Terminator[_]]
  final def asTerminator[T <: Entry] = this.asInstanceOf[Entry.Terminator[T]]
  
  override def hashCode = index.hashCode()
  override def equals(that: Any) = 
    that.isInstanceOf[AnyRef] && (this eq that.asInstanceOf[AnyRef])
  
  // Ordered
  override final def compare(that: Entry) =
    Math.signum(index - that.index).toInt
  
}

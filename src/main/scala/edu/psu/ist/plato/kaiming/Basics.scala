package edu.psu.ist.plato.kaiming

import utils.Indexed

import scala.collection.immutable.TreeSet

import scala.math.Ordering

trait Arch

abstract class Entry[A <: Arch] extends Indexed {
  
  override final def hashCode = index.hashCode()
  override final def equals(that: Any) = 
    that.isInstanceOf[AnyRef] && (this eq that.asInstanceOf[AnyRef])
    
  final def isTerminator: Boolean = this.isInstanceOf[Terminator[A]]
  final def asTerminator = this.asInstanceOf[Terminator[A]]
  
}

abstract class MachEntry[A <: Arch] extends Entry[A] {
  
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

class BBlock[A <: Arch](val parent: Procedure[A], val entries: Seq[Entry[A]],
    val label: Label) extends Iterable[Entry[A]] with Indexed {
  
  require(entries.size > 0, parent.label)
  
  def firstEntry = entries.head
  def lastEntry  = entries.last
  
  override def iterator = entries.iterator
  
  @inline final def index = firstEntry.index
  
  @inline final override def equals(obj: Any) = obj match {
    case bb: BBlock[A] => index == bb.index
    case _ => false
  }
  
  @inline final override def hashCode = index.hashCode

  override def toString = "[0x" + index.toHexString + ": " + label.name + "]"

}

class MachBBlock[A <: Arch](override val parent: MachProcedure[A],
    override val entries: Vector[MachEntry[A]], override val label: Label)
    extends BBlock[A](parent, entries, label) with Iterable[MachEntry[A]] {
  
  override def iterator = entries.iterator
    
  override def firstEntry: MachEntry[A] = entries.head
  override def lastEntry: MachEntry[A] = entries.last

}

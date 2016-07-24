package edu.psu.ist.plato.kaiming

import scala.collection.immutable.TreeSet

import scala.math.Ordering

class BBlock[A <: Arch](val parent: Procedure[A], val entries: Seq[Entry[A]],
    val label: Label) extends Iterable[Entry[A]] with Ordered[BBlock[A]] {
  
  def firstEntry = entries.head
  def lastEntry  = entries.last
  
  override def iterator = entries.iterator
  
  def index = firstEntry.index
  override def equals(obj: Any) = obj match {
    case bb: BBlock[_] => index == bb.index
    case _ => false
  }
  override def hashCode = index.hashCode()

  override def compare(b: BBlock[A]) = {
    (firstEntry.index - b.firstEntry.index).toInt
  }

}

class MachBBlock[A <: MachArch](override val parent: MachProcedure[A],
    override val entries: Seq[MachEntry[A]], override val label: Label)
    extends BBlock[A](parent, entries, label) with Iterable[MachEntry[A]] {
  
  override def iterator = entries.iterator
    
  override def firstEntry: MachEntry[A] = entries.head
  override def lastEntry: MachEntry[A] = entries.last
  
  def compare(that: MachBBlock[A]) =
    (firstEntry.index - that.firstEntry.index).toInt
  
}


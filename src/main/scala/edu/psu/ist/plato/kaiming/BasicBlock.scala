package edu.psu.ist.plato.kaiming

import utils.Indexed

import scala.collection.immutable.TreeSet

import scala.math.Ordering

class BBlock[A <: Arch](val parent: Procedure[A], val entries: Seq[Entry[A]],
    val label: Label) extends Iterable[Entry[A]] with Indexed {
  
  require(entries.size > 0, parent.label)
  
  def firstEntry = entries.head
  def lastEntry  = entries.last
  
  override def iterator = entries.iterator
  
  @inline final val index = firstEntry.index
  
  @inline final override def equals(obj: Any) = obj match {
    case bb: BBlock[A] => index == bb.index
    case _ => false
  }
  
  @inline final override def hashCode = index.hashCode

  override def toString = "[0x" + index.toHexString + ": " + label.name + "]"

}

class MachBBlock[A <: MachArch](override val parent: MachProcedure[A],
    override val entries: Vector[MachEntry[A]], override val label: Label)
    extends BBlock[A](parent, entries, label) with Iterable[MachEntry[A]] {
  
  override def iterator = entries.iterator
    
  override def firstEntry: MachEntry[A] = entries.head
  override def lastEntry: MachEntry[A] = entries.last

}


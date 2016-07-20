package edu.psu.ist.plato.kaiming

import scala.collection.immutable.TreeSet

case class BasicBlock[A <: Arch](val parent: Procedure[A], val entries: Seq[Entry[A]],
    val label: Label) extends Iterable[Entry[A]] with Ordered[BasicBlock[A]] {
  
  def firstEntry = entries.head
  def lastEntry  = entries.last
  
  override def iterator = entries.iterator
  
  def index = firstEntry.index
  override def compare(that: BasicBlock[A]) =
    (firstEntry.index - that.firstEntry.index).toInt
  override def equals(obj: Any) = obj match {
    case bb: BasicBlock[_] => index == bb.index
    case _ => false
  }
  override def hashCode = index.hashCode()
  
  def numOfEnries = entries.size

}    


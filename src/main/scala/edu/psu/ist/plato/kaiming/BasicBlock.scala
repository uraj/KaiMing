package edu.psu.ist.plato.kaiming

import scala.collection.immutable.TreeSet

case class BasicBlock[T <: Entry](val parent: Procedure[T], val entries: Seq[T],
    val label: Label) extends Iterable[T] with Ordered[BasicBlock[T]] {
  
  def firstEntry = entries.head
  def lastEntry  = entries.last
  
  override def iterator = entries.iterator
  
  def index = firstEntry.index
  override def compare(that: BasicBlock[T]) =
    (firstEntry.index - that.firstEntry.index).toInt
  override def equals(obj: Any) = obj match {
    case bb: BasicBlock[_] => index == bb.index
    case _ => false
  }
  override def hashCode = index.hashCode()
  
  def numOfEnries = entries.size

}    


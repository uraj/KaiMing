package edu.psu.ist.plato.kaiming

abstract class Procedure[T <: Entry](val label: Label, initEntries: Seq[T]) {

  val cfg = new CFG(this, initEntries)
  def name = label.name
  def entries: List[T] = cfg.entries
  def deriveLabelForIndex(index: Long): Label
  
}

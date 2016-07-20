package edu.psu.ist.plato.kaiming

abstract class Procedure[A <: Arch] {

  val label: Label
  val cfg: CFG[A]
  def name = label.name
  def entries: List[Entry[A]] = cfg.entries
  def deriveLabelForIndex(index: Long): Label
  
}

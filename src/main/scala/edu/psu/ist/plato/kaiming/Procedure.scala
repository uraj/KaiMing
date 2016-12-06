package edu.psu.ist.plato.kaiming

import ir.{Context, IRCfg}

abstract class Procedure[A <: Arch] {

  def label: Label
  def cfg: Cfg[A, _ <: BBlock[A]]
  def name = label.name
  def entries: Vector[Entry[A]]
  def index = cfg.entryBlock.index
  def deriveLabelForIndex(index: Long): Label = Label("_sub_" + index.toHexString)

}

abstract class MachProcedure[A <: Arch](val entries: Vector[MachEntry[A]],
    trimIsolated: Boolean = false) extends Procedure[A] {
  
  def mach: Machine[A]
  override val cfg = MachCfg(this)
  
}
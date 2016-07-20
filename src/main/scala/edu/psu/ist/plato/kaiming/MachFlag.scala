package edu.psu.ist.plato.kaiming

import enumeratum.EnumEntry

trait MachFlag[A <: Arch] extends EnumEntry {
  def name: String
  def index: Int
}
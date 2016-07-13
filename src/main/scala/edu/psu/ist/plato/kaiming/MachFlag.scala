package edu.psu.ist.plato.kaiming

import edu.psu.ist.plato.kaiming.Machine.Arch

trait MachFlag {
  def name: String
  def arch: Arch
  def index: Int
}
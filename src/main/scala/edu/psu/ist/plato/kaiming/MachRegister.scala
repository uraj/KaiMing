package edu.psu.ist.plato.kaiming

import edu.psu.ist.plato.kaiming.Machine.Arch

abstract class MachRegister {
  def name: String
  def arch: Arch
  def sizeInBits: Int
  def containingRegister: MachRegister
  def subsumedRegisters: Set[MachRegister]
  
  override def equals(that: Any): Boolean
  override def hashCode: Int
  
}
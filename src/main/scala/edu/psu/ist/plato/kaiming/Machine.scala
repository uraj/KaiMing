package edu.psu.ist.plato.kaiming

import edu.psu.ist.plato.kaiming.Arch._

import edu.psu.ist.plato.kaiming.ir.Stmt

import edu.psu.ist.plato.kaiming.aarch64.AArch64Machine

object Machine {

  val aarch64 = AArch64Machine.instance

}

abstract class Machine[A <: MachArch] {

  def liftToIR(cfg: CFG[A]): CFG[KaiMing]
  val returnRegister: MachRegister[A]
}

trait MachFlag[A <: MachArch] extends enumeratum.EnumEntry {
  
  def name: String
  def index: Int
  
}

abstract class MachRegister[A <: MachArch] {
  
  def name: String
  def sizeInBits: Int
  def containingRegister: MachRegister[A]
  def subsumedRegisters: Set[MachRegister[A]]
  
  override def equals(that: Any): Boolean
  override def hashCode: Int
  
}

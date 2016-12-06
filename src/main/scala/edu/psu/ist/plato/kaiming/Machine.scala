package edu.psu.ist.plato.kaiming

import ir._
import aarch64.AArch64Machine

abstract class Machine[A <: Arch] {

  val returnRegister: MachRegister[A]
  val wordSizeInBits: Int
  val registers: Set[MachRegister[A]]
  
  def toIRStatements(inst: MachEntry[A], builder: IRBuilder[A]): IRBuilder[A]

}

trait MachFlag[A <: Arch] extends enumeratum.EnumEntry {
  
  def name: String
  def index: Int
  
}

abstract class MachRegister[A <: Arch] {
  
  def name: String
  def sizeInBits: Int
  def containingRegister: MachRegister[A]
  def subsumedRegisters: Set[MachRegister[A]]
  
  override def equals(that: Any): Boolean
  override def hashCode: Int
  
}

package edu.psu.ist.plato.kaiming

package object aarch64 {
  
  sealed trait AArch64 extends Arch
  import scala.language.implicitConversions
  implicit def toAArch64Inst(e: MachEntry[AArch64]) = e.asInstanceOf[Instruction]
  
}
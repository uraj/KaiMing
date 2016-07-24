package edu.psu.ist.plato.kaiming

package object aarch64 {
  
  import edu.psu.ist.plato.kaiming.Arch.AArch64
  import scala.language.implicitConversions
  implicit def toAArch64Inst(e: MachEntry[AArch64]) = e.asInstanceOf[Instruction]
  
}
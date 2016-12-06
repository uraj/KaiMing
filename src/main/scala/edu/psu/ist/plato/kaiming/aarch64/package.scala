package edu.psu.ist.plato.kaiming

package object aarch64 {
  
  sealed trait AArch64 extends Arch
  import scala.language.implicitConversions
  implicit def toAArch64Inst(e: MachEntry[AArch64]) = e.asInstanceOf[Instruction]
  
  sealed abstract class Flag(val name: String, val index: Int) extends MachFlag[AArch64]

  object Flag {
    case object C extends Flag("C", 0)
    case object Z extends Flag("Z", 1)
    case object N extends Flag("N", 2)
    case object V extends Flag("V", 3)
  }
  
  class Function(override val label: Label, insts: Vector[Instruction])
      extends MachProcedure[AArch64](insts) {
  
    val mach = AArch64Machine
    
  }
  
}
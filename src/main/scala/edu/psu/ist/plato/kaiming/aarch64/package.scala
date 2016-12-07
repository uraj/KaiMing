package edu.psu.ist.plato.kaiming

package object aarch64 {
  
  sealed trait AArch64 extends Arch
  import scala.language.implicitConversions
  implicit def toAArch64Inst(e: Entry[AArch64]) = e.asInstanceOf[Instruction]
  
  sealed abstract class Flag(val name: String, val index: Int) extends MachFlag[AArch64]

  object Flag {
    case object C extends Flag("C", 0)
    case object Z extends Flag("Z", 1)
    case object N extends Flag("N", 2)
    case object V extends Flag("V", 3)
  }
  
  class Function(val label: Label, val entries: Vector[Instruction])
      extends Procedure[AArch64] {
  
    val cfg = MachCfg(this)

  }
  
  implicit val aarch64Machine: Machine[AArch64] = AArch64Machine
  
}
package io.github.uraj.kaiming

package object arm {
  
  import io.github.uraj.kaiming.Arch
  sealed trait ARM extends Arch
  
  import scala.language.implicitConversions
  implicit def toARMInstruction(e: Entry[ARM]) = e.asInstanceOf[Instruction]
  
  sealed abstract class Flag(val name: String, val index: Int) extends MachFlag[ARM]

  object Flag {
    case object C extends Flag("C", 0)
    case object Z extends Flag("Z", 1)
    case object N extends Flag("N", 2)
    case object V extends Flag("V", 3)
  }
  
  class Function(val label: Label, val entries: Vector[Instruction])
      extends Procedure[ARM] {
  
    val cfg = MachCfg(this)
  
  }
  
  implicit val armMachine: Machine[ARM] = ARMMachine 
  
}
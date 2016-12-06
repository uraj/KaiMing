package edu.psu.ist.plato.kaiming

package object arm {
  
  import edu.psu.ist.plato.kaiming.Arch
  sealed trait ARM extends Arch
  
  import scala.language.implicitConversions
  implicit def toARMInstruction(e: MachEntry[ARM]) = e.asInstanceOf[Instruction]
  
  sealed abstract class Flag(val name: String, val index: Int) extends MachFlag[ARM]

  object Flag {
    case object C extends Flag("C", 0)
    case object Z extends Flag("Z", 1)
    case object N extends Flag("N", 2)
    case object V extends Flag("V", 3)
  }
  
  class Function(override val label: Label, insts: Vector[Instruction])
      extends MachProcedure[ARM](insts) {
  
    val mach = ARMMachine
  
  }
  
}
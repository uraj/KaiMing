package edu.psu.ist.plato.kaiming

package object arm {
  
  import edu.psu.ist.plato.kaiming.Arch
  sealed trait ARM extends Arch
  
  import scala.language.implicitConversions
  implicit def toARMInstruction(e: MachEntry[ARM]) = e.asInstanceOf[Instruction]
  
}
package edu.psu.ist.plato.kaiming.arm

import edu.psu.ist.plato.kaiming.Label
import edu.psu.ist.plato.kaiming.MachProcedure

class Function(override val label: Label, insts: Vector[Instruction])
    extends MachProcedure[ARM](insts) {
  
  val mach = ARMMachine
  
}
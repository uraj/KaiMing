package edu.psu.ist.plato.kaiming.arm

import edu.psu.ist.plato.kaiming.Label
import edu.psu.ist.plato.kaiming.MachProcedure
import edu.psu.ist.plato.kaiming.Arch.ARM

class Function(override val label: Label, insts: Seq[Instruction]) extends MachProcedure[ARM](insts) {
  
  val mach = ARMMachine
  
}
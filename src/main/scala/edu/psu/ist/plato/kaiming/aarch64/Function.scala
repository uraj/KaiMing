package edu.psu.ist.plato.kaiming.aarch64

import edu.psu.ist.plato.kaiming._

class Function(override val label: Label, insts: Seq[Instruction])
  extends MachProcedure[Arch.AArch64](insts) {

  val mach = AArch64Machine
  
  override def deriveLabelForIndex(index: Long) = {
    Label("_sub_" + index.toHexString)
  }
  
}
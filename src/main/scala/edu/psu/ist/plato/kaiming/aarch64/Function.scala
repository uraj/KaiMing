package edu.psu.ist.plato.kaiming.aarch64

import edu.psu.ist.plato.kaiming.Procedure
import edu.psu.ist.plato.kaiming.Label
import edu.psu.ist.plato.kaiming.BasicBlock
import edu.psu.ist.plato.kaiming.CFG
import edu.psu.ist.plato.kaiming.Arch.AArch64

class Function(override val label: Label, insts: Seq[Instruction])
  extends Procedure[AArch64] {

  override val cfg = new CFG(this, insts)
  override def deriveLabelForIndex(index: Long) = {
    Label("_sub_" + index.toHexString, index)
  }
  
}
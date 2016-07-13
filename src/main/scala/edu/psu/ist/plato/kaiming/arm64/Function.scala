package edu.psu.ist.plato.kaiming.arm64

import edu.psu.ist.plato.kaiming.Procedure
import edu.psu.ist.plato.kaiming.Label
import edu.psu.ist.plato.kaiming.BasicBlock

case class Function(l: Label, insts: Seq[Instruction])
  extends Procedure[Instruction](l, insts) {

  override def deriveLabelForIndex(index: Long) = {
    Label("_sub_" + index.toHexString, index)
  }
  
}
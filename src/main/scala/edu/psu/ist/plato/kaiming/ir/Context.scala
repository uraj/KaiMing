package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming._

class Context private (val proc: MachProcedure[_])
    extends Procedure[Arch.KaiMing] {

  override val label = proc.label
  override val cfg = proc.liftCFGToIR
  
  override def deriveLabelForIndex(index: Long) = {
    Label("_sub_" + index.toHexString, index)
  }
  
}
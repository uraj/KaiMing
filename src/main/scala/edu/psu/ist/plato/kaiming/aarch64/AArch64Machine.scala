package edu.psu.ist.plato.kaiming.aarch64

import edu.psu.ist.plato.kaiming.CFG
import edu.psu.ist.plato.kaiming.Machine
import edu.psu.ist.plato.kaiming.Arch.AArch64

object AArch64Machine {
  val instance: Machine[AArch64] = new AArch64Machine
}

class AArch64Machine private extends Machine[AArch64] {
  
  override def liftToIR(cfg: CFG[AArch64]) = null
  override val returnRegister = Register.get(Register.Id.X0, None)
  
}
package edu.psu.ist.plato.kaiming

import edu.psu.ist.plato.kaiming.Arch._

import edu.psu.ist.plato.kaiming.ir.Stmt

import edu.psu.ist.plato.kaiming.aarch64.AArch64Machine
import edu.psu.ist.plato.kaiming.x86.X86Machine

object Machine {

  val aarch64 = AArch64Machine.instance

}


abstract class Machine[A <: MachArch] {

  def liftToIR(cfg: CFG[A]): CFG[KaiMing]
  
}

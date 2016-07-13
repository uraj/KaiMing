package edu.psu.ist.plato.kaiming

import edu.psu.ist.plato.kaiming.arm64.ARM64Machine
import edu.psu.ist.plato.kaiming.x86.X86Machine

object Machine {
  sealed trait Arch
  object Arch {
    case object ARM64 extends Arch
    case object X86 extends Arch
  }
}

class Machine(architecture: Machine.Arch) {

  val arch = architecture
  
  val arm64 = ARM64Machine.instance

}

package edu.psu.ist.plato.kaiming.arm64

import edu.psu.ist.plato.kaiming.Machine

object ARM64Machine {
  val instance = new ARM64Machine
}

class ARM64Machine private extends Machine(Machine.Arch.ARM64) {
  
}
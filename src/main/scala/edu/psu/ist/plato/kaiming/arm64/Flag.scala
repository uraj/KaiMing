package edu.psu.ist.plato.kaiming.arm64

import edu.psu.ist.plato.kaiming.MachFlag
import edu.psu.ist.plato.kaiming.Machine

sealed abstract class Flag(val name: String, val index: Int) extends MachFlag {
  val arch = Machine.Arch.ARM64
}

object Flag {
  case object C extends Flag("C", 0)
  case object Z extends Flag("Z", 1)
  case object N extends Flag("N", 2)
  case object V extends Flag("V", 3)
}
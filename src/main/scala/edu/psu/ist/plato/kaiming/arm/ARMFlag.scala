package edu.psu.ist.plato.kaiming.arm

import edu.psu.ist.plato.kaiming.MachFlag
import edu.psu.ist.plato.kaiming.Machine
import edu.psu.ist.plato.kaiming.Arch

sealed abstract class Flag(val name: String, val index: Int)
    extends MachFlag[ARM]

object Flag {
  case object C extends Flag("C", 0)
  case object Z extends Flag("Z", 1)
  case object N extends Flag("N", 2)
  case object V extends Flag("V", 3)
}
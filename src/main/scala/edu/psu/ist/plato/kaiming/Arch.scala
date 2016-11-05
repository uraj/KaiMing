package edu.psu.ist.plato.kaiming

sealed abstract class Arch
sealed abstract class MachArch(val wordSizeInBits: Int) extends Arch

object Arch {
  
  sealed abstract class AArch64 extends MachArch(64)
  object AArch64 extends AArch64
  
  sealed abstract class ARM extends MachArch(32)
  object ARM extends ARM
  
  final abstract class KaiMing extends Arch

}
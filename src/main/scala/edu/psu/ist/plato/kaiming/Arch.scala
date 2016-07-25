package edu.psu.ist.plato.kaiming

sealed abstract class Arch
sealed abstract class MachArch extends Arch

object Arch {
  
  final abstract class AArch64 extends MachArch
  final abstract class ARM extends MachArch
  final abstract class X86 extends MachArch
  
  final abstract class KaiMing extends Arch
  
}


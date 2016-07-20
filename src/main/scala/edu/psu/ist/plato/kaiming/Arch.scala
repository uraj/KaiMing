package edu.psu.ist.plato.kaiming

sealed abstract class Arch
sealed abstract class MachArch extends Arch

object Arch {
  
  final class AArch64 extends MachArch
  final class X86 extends MachArch
  
  final class KaiMing extends Arch
  
}


package edu.psu.ist.plato.kaiming

sealed abstract class Arch
sealed abstract class MachineArch extends Arch

object Arch {
  
  
  
  final class AArch64 extends MachineArch
  final class X86 extends MachineArch
  
  final class KaiMing extends Arch
  
}


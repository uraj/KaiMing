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

import java.io.File
import edu.psu.ist.plato.kaiming.aarch64.AArch64Parser

object Main {
  def main(args: Array[String]): Unit = {
    val file = new File("/home/pxw172/git/KaiMing/src/test/resources/test/aarch64/test-03.s") 
    val funcs = AArch64Parser.parseFile(file)
    println(funcs.length + " functions successfully parsed")
  }
}
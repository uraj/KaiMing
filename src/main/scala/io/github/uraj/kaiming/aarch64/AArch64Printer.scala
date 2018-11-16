package io.github.uraj.kaiming.aarch64

import java.io.PrintStream
import java.io.OutputStream

import io.github.uraj.kaiming.Label
import io.github.uraj.kaiming.BBlock
import io.github.uraj.kaiming.Cfg
import io.github.uraj.kaiming.utils.Exception

object AArch64Printer {
  
  val out = new AArch64Printer(Console.out)
  val err = new AArch64Printer(Console.err)
  
}

final class AArch64Printer(ps: OutputStream) extends PrintStream(ps) {
  
  private def printSignedHex(value: Long) {
    if (value < 0)
      print("-0x%x".format(-value))
    else
      print("0x%x".format(value))
  }
  
  def printOpImmediate(imm: Immediate) {
    print('#')
    printSignedHex(imm.value)
    if (imm.lShift != 0)
      print(s", LSL #${imm.lShift}")
  }
  
  def printOpRegister(reg: Register) {
    print(reg.name)
  }
  
  def printOpShiftedRegister(sreg: ModifiedRegister) {
    val mod = sreg.modifier
    printOpRegister(sreg.reg)
    print(", ")
    print(mod.getClass.getSimpleName.toUpperCase)
    print(", ")
    mod match {
      case shift: Shift =>
        print('#')
        print(shift.value)
      case ext: RegExtension =>
        if (ext.lsl != 0) {
          print('#')
          print(ext.lsl)
        }
    }
  }
  
  def printOpMemory(mem: Memory) {
    print('[')
    mem.base match {
      case Some(base) => printOpRegister(base)
      case None =>
    }
    mem.off match {
      case Some(off) =>
        print(", ")
        off match {
          case Left(imm) => printOpImmediate(imm)
          case Right(sreg) => printOpShiftedRegister(sreg)
        }
      case None =>
    }
    print(']')
  }
  
  def printOperand(o: Operand) = {
    o match {
      case r: Register => printOpRegister(r)
      case s: ModifiedRegister => printOpShiftedRegister(s)
      case i: Immediate => printOpImmediate(i)
      case m: Memory => printOpMemory(m)
    }
  }
  
  def printInstruction(i: Instruction) {
    print(i.mnem)
    print('\t')
    i match {
      case b: BranchInst =>
        if (!b.isReturn) {
          b.relocatedTarget match {
            case Some(t) => print(t.label.name)
            case None => 
              b.target match {
                case r: Register => printOpRegister(r)
                case Memory(base, Some(Left(off))) => 
                  printSignedHex(off.value)
                case _ => Exception.unreachable()
            }
          }
        }
      case _ => {
        import AddressingMode._
        val (indexingOperand, addressingMode) = 
          if (i.isInstanceOf[LoadStoreInst]) {
            val lsi = i.asInstanceOf[LoadStoreInst]
            (lsi.indexingOperandIndex, lsi.addressingMode)
          } else {
            (-1, Regular)
          }
        (0 until i.operands.size).foreach {
          n => 
            if (n == indexingOperand) {
              addressingMode match {
                case PreIndex =>
                  printOperand(i.operands(n))
                  print('!')
                case PostIndex => {
                  val next = i.operands(n).asMemory
                  printOperand(next.base.get)
                  print(", ")
                  printOpImmediate(next.off.get.left.get)
                }
                case Regular =>
                  printOperand(i.operands(n))
              }
            } else {
              printOperand(i.operands(n))
            }
            if (n != i.operands.size - 1)
              print(", ")
        }
      }
    }
    if (i.isInstanceOf[SelectInst]) {
      print(", ")
      print(i.asInstanceOf[SelectInst].condition.entryName)
    } else if (i.isInstanceOf[CondCompareInst]) {
      print(", ")
      print(i.asInstanceOf[CondCompareInst].condition.entryName)
    }
  }
  
  def printBasicBlock(bb: BBlock[AArch64]) {
    println(bb.label)
    bb.foreach { i => print('\t'); printInstruction(i); println() }
  }
  
  def printCFG(cfg: Cfg[AArch64]) {
    println(cfg.parent.label)
    for (bb <- cfg) { printBasicBlock(bb) }
  }
  
  def printFunction(f: Function) {
    println(f.label)
    f.entries.foreach { i => printInstruction(i); println() }
  }
  
}
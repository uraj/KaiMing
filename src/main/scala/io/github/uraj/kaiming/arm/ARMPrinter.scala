package io.github.uraj.kaiming.arm

import java.io.PrintStream
import java.io.OutputStream

import io.github.uraj.kaiming.Label
import io.github.uraj.kaiming.BBlock
import io.github.uraj.kaiming.Cfg
import io.github.uraj.kaiming.utils.Exception

object ARMPrinter {
  
  val out = new ARMPrinter(Console.out)
  val err = new ARMPrinter(Console.err)
  
}

final class ARMPrinter(ps: OutputStream) extends PrintStream(ps) {
  
  private def printSignedHex(value: Long) {
    if (value < 0)
      print("-0x%x".format(-value))
    else
      print("0x%x".format(value))
  }
  
  def printOpImmediate(imm: Immediate) {
    print('#')
    printSignedHex(imm.value)
  }
  
  def printOpRegister(reg: Register) {
    print(reg.id.entryName)
    reg.shift match {
      case Some(shift) =>
        print(", ")
        print(shift.getClass.getSimpleName.toUpperCase)
        print(", ")
        print(shift.value)
      case None => 
    }
  }
  
  def printOpMemory(mem: Memory) {
    mem.base match {
      case Some(base) =>
        print('[')
        printOpRegister(base)
        mem.off match {
          case Left(imm) =>
            if (imm != 0) {
              print(", #")
              printSignedHex(imm)
            }
          case Right(reg) =>
            print(", ")
            if (reg._1 == Negative)
              print('-')
            printOpRegister(reg._2)
        }
        print(']')
      case None =>
        printSignedHex(mem.off.left.get)
    }
  }
  
  def printOperand(o: Operand) = {
    o match {
      case r: Register => printOpRegister(r)
      case i: Immediate => printOpImmediate(i)
      case m: Memory => printOpMemory(m)
    }
  }
  
  def printInstruction(i: Instruction) {
    print(i.opcode.rawcode)
    print('\t')
    val operands = i.operands 
    i match {
      case b: BranchInst =>
        if (!b.isReturn) {
          b.relocatedTarget match {
            case Some(b) => print(b.label.name)
            case None => 
              b.target match {
                case r: Register => printOpRegister(r)
                case Memory(base, Left(off)) => 
                  printSignedHex(off)
                case _ => Exception.unreachable()
            }
          }
        }
      case lsm if (lsm.isInstanceOf[LoadMultipleInst] || lsm.isInstanceOf[StoreMultipleInst]) => {
        printOpRegister(operands(0).asMemory.base.get)
        print("!, {")
        for (op <- operands.slice(1, operands.size - 1)) {
          printOperand(op)
          print(", ")
        }
        printOperand(i.operands.last)
        print("}")
      }
      case m: MoveInst if m.opcode.mnemonic == Opcode.Mnemonic.LDR =>
        printOpRegister(m.dest)
        print(", =")
        printSignedHex(m.src.asImmediate.value)
      case _ => {
        import AddressingMode._
        val (indexingOperand, addressingMode) = 
          if (i.isInstanceOf[LoadStoreInst]) {
            val lsi = i.asInstanceOf[LoadStoreInst]
            (lsi.indexingOperandIndex, lsi.addressingMode)
          } else {
            (-1, Regular)
          }
        (0 until operands.size).foreach {
          n => 
            if (n == indexingOperand) {
              addressingMode match {
                case PreIndex => {
                  printOperand(operands(n))
                  print('!')
                }
                case PostIndex => {
                  val next = operands(n).asMemory
                  printOperand(next.base.get)
                  print(", #")
                  printSignedHex(next.off.left.get)
                }
                case Regular =>
                  printOperand(operands(n))
              }
            } else {
              printOperand(operands(n))
            }
            if (n != operands.size - 1)
              print(", ")
        }
      }
    }
  }
  
  def printBasicBlock(bb: BBlock[ARM]) {
    println(bb.label)
    bb.foreach { i => print('\t'); printInstruction(i); println() }
  }
  
  def printCFG(cfg: Cfg[ARM]) {
    println(cfg.parent.label)
    for (bb <- cfg) { printBasicBlock(bb) }
  }
  
  def printFunction(f: Function) {
    println(f.label)
    f.entries.foreach { i => printInstruction(i); println() }
  }
  
}
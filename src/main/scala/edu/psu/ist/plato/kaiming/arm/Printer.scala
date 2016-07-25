package edu.psu.ist.plato.kaiming.arm

import java.io.PrintStream
import java.io.OutputStream

import edu.psu.ist.plato.kaiming.Label
import edu.psu.ist.plato.kaiming.MachBBlock
import edu.psu.ist.plato.kaiming.Cfg
import edu.psu.ist.plato.kaiming.Arch.ARM

import edu.psu.ist.plato.kaiming.exception._

object Printer {
  
  val out = new Printer(Console.out)
  val err = new Printer(Console.err)
  
}

final class Printer(ps: OutputStream) extends PrintStream(ps) {
  
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
          case Right(reg) => printOpRegister(reg)
        }
      case None =>
    }
    print(']')
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
    i match {
      case b: BranchInst =>
        if (!b.isReturn) {
          b.relocatedTarget match {
            case Some(b) => print(b.label.name)
            case None => 
              b.target match {
                case r: Register => printOpRegister(r)
                case Memory(base, Some(Left(off))) => 
                  printSignedHex(off.value)
                case _ => throw new UnreachableCodeException()
            }
          }
        }
      case _ => {
        import AddressingMode._
        val (indexingOperand, addressingMode) = 
          if (i.isInstanceOf[LoadStoreInst]) {
            val lsi = i.asInstanceOf[LoadStoreInst]
            val m = lsi.addressingMode
            val idx = lsi.indexingOperand
            (if (m == Regular) -1 else idx, m)
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
                  throw new UnreachableCodeException()
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
    }
  }
  
  def printBasicBlock(bb: MachBBlock[ARM]) {
    println(bb.label)
    bb.foreach { i => print('\t'); printInstruction(i); println() }
  }
  
  def printCFG(cfg: Cfg[ARM, MachBBlock[ARM]]) {
    println(cfg.parent.label)
    for (bb <- cfg) { printBasicBlock(bb) }
  }
  
  def printFunction(f: Function) {
    println(f.label)
    f.entries.foreach { i => printInstruction(i); println() }
  }
  
}
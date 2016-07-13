package edu.psu.ist.plato.kaiming.arm64

import java.io.PrintWriter
import java.io.OutputStream

import edu.psu.ist.plato.kaiming.Label
import edu.psu.ist.plato.kaiming.BasicBlock
import edu.psu.ist.plato.kaiming.CFG

import edu.psu.ist.plato.kaiming.exception._

class Printer(ps: OutputStream, val parseMode: Boolean) extends PrintWriter(ps, true) {
  
  def this(ps: OutputStream) = this(ps, false)
  
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
        print(shift.ty.entryName)
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
    if (parseMode) {
      printSignedHex(i.addr)
      print('\t')
    }
    print(i.opcode.rawcode)
    print('\t')
    i match {
      case b: BranchInst =>
        if (!b.isReturn) {
          b.relocatedTarget match {
            case Some(b) => printLabel(b.label)
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
        import Instruction.AddressingMode._
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
  
  def printLabel(l: Label) {
    if (parseMode) {
      printSignedHex(l.addr)
      print('\t')
    }
    print(l.name)
  }
  
  def printBasicBlock(bb: BasicBlock[Instruction]) {
    if (!parseMode) {
      printLabel(bb.label)
      println(':')
    }
    bb.foreach { i => printInstruction(i); println() }
  }
  
  def printCFG(cfg: CFG[Instruction]) {
    printLabel(cfg.parent.label)
    println(':');
    cfg.foreach { bb => printBasicBlock(bb) }
  }
  
  def printFunction(f: Function) {
    printLabel(f.label)
    println(':')
    f.entries.foreach { i => printInstruction(i); println() }
  }
  
}
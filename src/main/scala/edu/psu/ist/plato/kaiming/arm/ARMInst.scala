package edu.psu.ist.plato.kaiming.arm

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.ARM
import edu.psu.ist.plato.kaiming.utils.Exception

import enumeratum._

sealed trait AddressingMode
object AddressingMode {
  
  case object PreIndex extends AddressingMode
  case object PostIndex extends AddressingMode
  case object Regular extends AddressingMode
  
}

object Instruction {
  
  import scala.language.implicitConversions
  implicit def toInstruction(entry: Entry[ARM]) = 
    entry.asInstanceOf[Instruction]
  
  private def resizeIfImm(op: Operand, sizeInBits: Int): Operand =
    if (op.isImmediate) op.asImmediate.resize(sizeInBits) else op
  
  def create(addr: Long, opcode: Opcode, oplist: Vector[Operand], preidx: Boolean): Instruction = {
    import edu.psu.ist.plato.kaiming.arm.Opcode.Mnemonic._
    opcode.mnemonic match {
      case ADD | SUB | MUL | SDIV | UDIV | ASR | LSL | LSR | ORR | ORN | AND | BIC | EOR =>
        require((oplist.length == 3 && oplist(1).isRegister) || oplist.length == 2)
        require(oplist(0).isRegister)
        val rd = oplist(0).asRegister 
        if (oplist.length == 3)
          BinaryArithInst(addr, opcode, rd, oplist(1).asRegister,
              resizeIfImm(oplist(2), rd.sizeInBits))
        else
          BinaryArithInst(addr, opcode, rd, rd, resizeIfImm(oplist(1), rd.sizeInBits))
      case MULL =>
        require(oplist.length == 4)
        for(op <- oplist)
          require(op.isRegister)
        LongMulInst(addr, opcode, oplist(0).asRegister, oplist(1).asRegister,
            oplist(2).asRegister, oplist(3).asRegister)
      case NOT | CLZ | RRX | ADR =>
        require(oplist.length == 2)
        require(oplist(0).isRegister)
        UnaryArithInst(addr, opcode, oplist(0).asRegister, oplist(1))
      case LDR if oplist(1).isImmediate => // LDR Rx, =imm
        require(oplist.length == 2 && oplist(0).isRegister)
        MoveInst(addr, opcode, oplist(0).asRegister, oplist(1))
      case LDR | STR => {
        require(oplist.length == 2 || oplist.length == 3)
        require(oplist(0).isRegister && oplist(1).isMemory)
        val rd = oplist(0).asRegister
        val m = oplist(1).asMemory
        val (mem, mode) = 
        if (oplist.length == 3) {
          require(!preidx && oplist(2).isImmediate)
          val off = Left(oplist(2).asImmediate.value)
          (Memory(m.base, off), AddressingMode.PostIndex)
        } else {
          (m, if (preidx) AddressingMode.PreIndex else AddressingMode.Regular)
        }
        if (opcode.mnemonic == LDR)
          LoadInst(addr, opcode, rd, mem, mode)
        else
          StoreInst(addr, opcode, rd, mem, mode)
      }
      case PUSH | POP => {
        require(oplist.length >= 1)
        for (op <- oplist) require(op.isRegister)
        val base = Memory.get(Register.Id.SP)
        if (opcode.mnemonic == PUSH)
          StoreMultipleInst(addr, opcode, base, true,
              oplist.map(_.asRegister), AddressingMode.PreIndex)
        else
          LoadMultipleInst(addr, opcode, base, true,
              oplist.map(_.asRegister), AddressingMode.PreIndex)
      }
      case LDM | STM => {
        require(oplist.length >= 2)
        for (op <- oplist) require(op.isRegister)
        val base = Memory.get(oplist.head.asRegister)
        val amode = if (preidx) AddressingMode.PreIndex else AddressingMode.Regular
        if (opcode.mnemonic == STM)
          StoreMultipleInst(addr, opcode, base, preidx,
              oplist.tail.map(_.asRegister), amode)
        else 
          LoadMultipleInst(addr, opcode, base, preidx,
              oplist.tail.map(_.asRegister), amode)
      }
      case TST | CMP | CMN | TEQ =>
        require(oplist.length == 2)
        require(oplist(0).isRegister && (oplist(1).isRegister || oplist(1).isImmediate))
        val rd = oplist(0).asRegister
        CompareInst(addr, opcode, rd, resizeIfImm(oplist(1), rd.sizeInBits))
      case MOV | MOVT =>
        require(oplist.length == 2)
        require(oplist(0).isRegister)
        MoveInst(addr, opcode, oplist(0).asRegister, oplist(1))
      case EXT =>
        require(oplist.length == 2)
        require(oplist(0).isRegister && oplist(1).isRegister)
        ExtensionInst(addr, opcode, oplist(0).asRegister, oplist(1).asRegister)
      case BFX =>
        require(oplist.length == 4)
        require(oplist(0).isRegister && oplist(1).isRegister 
            && oplist(2).isImmediate && oplist(3).isImmediate)
        ExtractInst(addr, opcode, oplist(0).asRegister, 
            oplist(1).asRegister, oplist(2).asImmediate, oplist(3).asImmediate)
      case B | BL =>
        require(oplist.length <= 1)
        if (oplist.length == 1) {
          require(oplist(0).isMemory || oplist(0).isRegister)
          BranchInst(addr, opcode, oplist(0))
        } else {
          BranchInst(addr, opcode, Register.get("X30"))
        }
      case BFC =>
        require(oplist.length == 3)
        BitfieldClearInst(addr, opcode, oplist(0).asRegister, oplist(1).asImmediate,
            oplist(2).asImmediate)
      case BFI =>
        require(oplist.length == 4)
        BitfieldInsertInst(addr, opcode, oplist(0).asRegister, oplist(2).asRegister,
            oplist(1).asImmediate, oplist(2).asImmediate)
      case NOP => NopInst(addr, opcode)
    }
    
  }
     
}

sealed abstract class Instruction(oplist: Operand*)
  extends MachEntry[ARM] with Iterable[Operand] {
  
  val mach = ARMMachine
  
  val operands = oplist.toVector
  val addr: Long
  val opcode: Opcode
  
  protected final def operand(idx: Int) = operands(idx)
  protected final def numOfOperands = operands.length
  
  override def iterator = operands.iterator
    
  override val index = addr
  
}

case class BinaryArithInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, srcLeft: Register, srcRight: Operand)
  extends Instruction(dest, srcLeft, srcRight) {
  
  def updateFlags = opcode.rawcode.endsWith("S")
  
}

case class LongMulInst(override val addr: Long, override val opcode: Opcode,
    destHi: Register, destLow: Register, srcLeft: Register, srcRight: Register)
    extends Instruction(destHi, destLow, srcLeft, srcRight) {
  
  def isSigned = opcode.rawcode.startsWith("S")
  def doesAccumulate = opcode.rawcode.charAt(3) == 'A'
  
}

case class UnaryArithInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, src: Operand) extends Instruction(dest, src) {
  
  def updateFlags = opcode.rawcode.endsWith("S")
  
}

sealed trait Extension

object Extension {
  
  case object Signed extends Extension 
  case object Unsigned extends Extension 
  
}

case class ExtensionInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, src: Register) extends Instruction(dest, src) {
  
  val extension =
    if (opcode.rawcode.charAt(0) == 'S')
      Extension.Signed
    else
      Extension.Unsigned
  
}

case class ExtractInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, src: Register, lsb: Immediate, width: Immediate)
  extends Instruction(dest, src, lsb, width) {
  
  val extension =
    if (opcode.rawcode.charAt(0) == 'S')
      Extension.Signed
    else
      Extension.Unsigned
  
}

case class BitfieldInsertInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, src: Register, lsb: Immediate, width: Immediate)
    extends Instruction(dest, src, lsb, width)
    
case class BitfieldClearInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, lsb: Immediate, width: Immediate)
    extends Instruction(dest, lsb, width)

case class BranchInst(override val addr: Long, override val opcode: Opcode,
    target: Operand) extends Instruction(target) with Terminator[ARM] {
  
  def condition = opcode.condition
  val hasLink = opcode.mnemonic == Opcode.Mnemonic.BL
  
  override def isConditional = condition != Condition.AL
  override def isReturn = target.isRegister && target.asRegister.id == Register.Id.LR
  override def isCall = hasLink
  override def isIndirect = target.isRegister
  override def isTargetConcrete = target.isMemory
  override def targetIndex = 
    if (target.isRegister)
      throw new UnsupportedOperationException()
    else
      target.asMemory.off match {
        case Left(imm) => imm
        case _ => throw new UnsupportedOperationException()
    }
}

case class MoveInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, src: Operand) extends Instruction(dest, src) {
  
  val isMoveTop = opcode.mnemonic == Opcode.Mnemonic.MOVT
  def condition = opcode.condition
  val isConditional = condition != Condition.AL
  
}

sealed trait CompareCode
object CompareCode {

  case object Compare extends CompareCode
  case object Test extends CompareCode
  case object TestEq extends CompareCode
  case object CompareNeg extends CompareCode

}

case class CompareInst(override val addr: Long, override val opcode: Opcode,
    left: Register, right: Operand) extends Instruction(left, right) {
  val code = opcode.mnemonic match {
    case Opcode.Mnemonic.CMP => CompareCode.Compare
    case Opcode.Mnemonic.CMN => CompareCode.CompareNeg
    case Opcode.Mnemonic.TST => CompareCode.Test
    case Opcode.Mnemonic.TEQ => CompareCode.TestEq
    case _ => Exception.unreachable()
  }
}

sealed abstract class LoadStoreInst(oplist: Operand*) extends Instruction(oplist:_*) {
  
  val addressingMode: AddressingMode
  def indexingOperandIndex: Int
  def indexingOperand = operand(indexingOperandIndex).asMemory
  
}

case class LoadInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, mem: Memory, override val addressingMode: AddressingMode)
    extends LoadStoreInst(dest, mem) {
  
  override val indexingOperandIndex = 1
  
  val (loadSizeInBytes, extension) = opcode.rawcode.substring(3) match {
    case "B" => (1, Extension.Unsigned)
    case "SB" => (1, Extension.Signed) 
    case "H" => (2, Extension.Unsigned)
    case "SH" => (2, Extension.Signed)
    case "" => (4, Extension.Unsigned)
    case _ => Exception.unreachable()
  }
  
}

case class StoreInst(override val addr: Long, override val opcode: Opcode,
    src: Register, mem: Memory, override val addressingMode: AddressingMode)
    extends LoadStoreInst(src, mem) {
  
  override val indexingOperandIndex = 1
  
  val (loadSizeInBytes, extension) = opcode.rawcode.substring(3) match {
    case "B" => (1, Extension.Unsigned)
    case "SB" => (1, Extension.Signed) 
    case "H" => (2, Extension.Unsigned)
    case "SH" => (2, Extension.Signed)
    case "" => (4, Extension.Unsigned)
    case _ => Exception.unreachable()
  }
  
}

sealed trait LSMultipleMode extends EnumEntry

object LSMultipleMode extends Enum[LSMultipleMode] {
  
  def values = findValues
  
  case object FD extends LSMultipleMode
  case object ED extends LSMultipleMode
  case object IA extends LSMultipleMode
  case object IB extends LSMultipleMode
  case object DA extends LSMultipleMode
  case object DB extends LSMultipleMode
  case object FA extends LSMultipleMode
  case object EA extends LSMultipleMode
  
}

case class LoadMultipleInst(override val addr: Long, override val opcode: Opcode, base: Memory, preindex: Boolean,
    destList: Vector[Register], override val addressingMode: AddressingMode) extends LoadStoreInst((base +: destList):_*) {
  
  override val indexingOperandIndex = 0
  val mode = LSMultipleMode.withName(opcode.rawcode.substring(3))
  
}

case class StoreMultipleInst(override val addr: Long, override val opcode: Opcode, base: Memory, preindex: Boolean,
    srcList: Vector[Register], override val addressingMode: AddressingMode) extends LoadStoreInst((base +: srcList):_*) {

  override val indexingOperandIndex = 0
  val mode = LSMultipleMode.withName(opcode.rawcode.substring(3))
  
}

case class NopInst(override val addr: Long, override val opcode: Opcode)
    extends Instruction()


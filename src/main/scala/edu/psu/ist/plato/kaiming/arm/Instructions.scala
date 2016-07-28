package edu.psu.ist.plato.kaiming.arm

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.ARM
import edu.psu.ist.plato.kaiming.exception.UnreachableCodeException

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
  
  def create(addr: Long, opcode: Opcode, oplist: Vector[Operand], preidx: Boolean): Instruction = {
    import edu.psu.ist.plato.kaiming.arm.Opcode.Mnemonic._
    opcode.mnemonic match {
      case ADD | SUB | MUL | DIV | ASR | LSL | LSR | ORR | ORN | AND | BIC | EOR=> 
        require(oplist.length == 3 || oplist.length == 2)
        require(oplist(0).isRegister)
        if (oplist.length == 3)
          BinaryArithInst(addr, opcode, oplist(0).asRegister, oplist(1), oplist(2))
        else
          BinaryArithInst(addr, opcode, oplist(0).asRegister, oplist(0), oplist(1))
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
          val off = Left(oplist(2).asImmediate)
          (Memory(m.base, Some(off)), AddressingMode.PostIndex)
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
        CompareInst(addr, opcode, oplist(0).asRegister, oplist(1))
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
  
  // This overriding may not be necessary, as long as
  // we restrict one assembly unit per process
  override def equals(that: Any) = that match {
    case t: AnyRef => this eq t
    case _ => false
  }
  
}

case class BinaryArithInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, srcLeft: Operand, srcRight: Operand)
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

object BranchInst {
  import scala.collection.mutable.Map
  private val _belongs = Map[BranchInst, MachBBlock[ARM]]()
  private def loopUpRelocation(b: BranchInst) = _belongs.get(b)
  private def relocateTarget(b: BranchInst, bb: MachBBlock[ARM]) =
    _belongs += (b->bb)
}

case class BranchInst(override val addr: Long, override val opcode: Opcode,
    target: Operand) extends Instruction(target) with Terminator[ARM] {
  
  def condition = opcode.condition
  val hasLink = opcode.mnemonic == Opcode.Mnemonic.BL
  def dependentFlags = condition.dependentMachFlags
  
  override val isReturn = target.isRegister && target.asRegister.id == Register.Id.LR
  override val isCall = hasLink
  override val isIndirect = target.isRegister
  override val isTargetConcrete = target.isMemory
  override def targetIndex = 
    if (target.isRegister)
      throw new UnsupportedOperationException()
    else
      target.asMemory.off match {
        case Some(Left(imm)) => imm.value
        case _ => throw new UnsupportedOperationException()
    }
  override def relocate(target: MachBBlock[ARM]) = 
    BranchInst.relocateTarget(this, target)

  override def relocatedTarget = BranchInst.loopUpRelocation(this)
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
    case _ => throw new UnreachableCodeException()
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
    case _ => throw new UnreachableCodeException()
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
    case _ => throw new UnreachableCodeException()
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

/*
case class SelectInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, srcTrue: Register, srcFalse: Register, condition: Condition)
    extends Instruction(dest, srcTrue, srcFalse)  
*/

case class NopInst(override val addr: Long, override val opcode: Opcode)
    extends Instruction()


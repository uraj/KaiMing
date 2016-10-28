package edu.psu.ist.plato.kaiming.aarch64

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.AArch64
import edu.psu.ist.plato.kaiming.utils.Exception

sealed trait AddressingMode
object AddressingMode {
  
  case object PreIndex extends AddressingMode
  case object PostIndex extends AddressingMode
  case object Regular extends AddressingMode
  
}

object Instruction {
  
  import scala.language.implicitConversions
  implicit def toInstruction(entry: Entry[AArch64]) = 
    entry.asInstanceOf[Instruction]
  
  private def resizeIfImm(op: Operand, sizeInBits: Int): Operand =
    if (op.isImmediate) op.asImmediate.resize(sizeInBits) else op

  def create(addr: Long, opcode: Opcode, oplist: Vector[Operand],
      cond: Option[Condition], preidx: Boolean): Instruction = {
    val condition = cond.getOrElse(Condition.AL)
    import edu.psu.ist.plato.kaiming.aarch64.Opcode.Mnemonic._
    opcode.mnemonic match {
      case BinArith => 
        require((oplist.length == 3 && oplist(1).isRegister) || oplist.length == 2)
        val rd = oplist(0).asRegister
        if (oplist.length == 3)
          BinaryArithInst(addr, opcode, rd, oplist(1).asRegister, 
              resizeIfImm(oplist(2), rd.sizeInBits))
        else
          BinaryArithInst(addr, opcode, rd, rd, resizeIfImm(oplist(1), rd.sizeInBits))
      case PCRelative =>
        require(oplist.length == 2)
        PCRelativeInst(addr, opcode, oplist(0).asRegister,
            oplist(1).asImmediate.resize(AArch64.wordSizeInBits))
      case UnArith =>
        require(oplist.length == 2 && !oplist(1).isMemory)
        UnaryArithInst(addr, opcode, oplist(0).asRegister, oplist(1))
      case MADD | MSUB =>
        require(oplist.length == 4)
        TrinaryArithInst(addr, opcode, oplist(0).asRegister,
            oplist(1).asRegister, oplist(2).asRegister, oplist(3).asRegister)
      case MNEG =>
        require(oplist.length == 3)
        require(oplist.forall(_.isRegister))
        val zero = if (oplist(0).sizeInBits == 64) Register.Id.XZR else Register.Id.WZR  
        TrinaryArithInst(addr, Opcode("MSUB"), oplist(0).asRegister,
            oplist(1).asRegister, oplist(2).asRegister, zero)
      case LDR | STR => {
        require(oplist.length == 2 || oplist.length == 3)
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
      case LDP | STP => {
        require(oplist.length == 3 || oplist.length == 4)
        val rd1 = oplist(0).asRegister
        val rd2 = oplist(1).asRegister
        val m = oplist(2).asMemory
        val (mem, mode) = 
        if (oplist.length == 4) {
          require(!preidx && oplist(3).isImmediate)
          val off = Left(oplist(3).asImmediate)
          (Memory(m.base, Some(off)), AddressingMode.PostIndex)
        } else {
          (m, if (preidx) AddressingMode.PreIndex else AddressingMode.Regular)
        }
        if (opcode.mnemonic == LDP)
          LoadPairInst(addr, opcode, rd1, rd2, mem, mode)
        else
          StorePairInst(addr, opcode, rd1, rd2, mem, mode)
      }
      case STX =>
        require(oplist.length == 3)
        StoreExclusiveInst(addr, opcode, oplist(0).asRegister,
            oplist(1).asRegister, oplist(2).asMemory)
      case STXP =>
        require(oplist.length == 4)
        StorePairExclusiveInst(addr, opcode, oplist(0).asRegister,
            oplist(1).asRegister, oplist(2).asRegister, oplist(3).asMemory)
      case Compare =>
        require(oplist.length == 2 && !oplist(1).isMemory)
        val rd = oplist(0).asRegister
        CompareInst(addr, opcode, rd, resizeIfImm(oplist(1), rd.sizeInBits))
      case CondCompare =>
        require(oplist.length == 3 && cond.isDefined && !oplist(1).isMemory)
        val rd = oplist(0).asRegister
        CondCompareInst(addr, opcode, rd, resizeIfImm(oplist(1), rd.sizeInBits),
            oplist(2).asImmediate.value.toInt, condition)
      case UnSel =>
        require(oplist.length == 1)
        UnarySelectInst(addr, opcode, oplist(0).asRegister, condition)
      case BinSel =>
        require(oplist.length == 2)
        BinarySelectInst(addr, opcode, oplist(0).asRegister,
            oplist(1).asRegister, condition)
      case TriSel =>
        require(oplist.length == 3)
        TrinarySelectInst(addr, opcode, oplist(0).asRegister,
            oplist(1).asRegister, oplist(2).asRegister, condition)
      case Move =>
        require(oplist.length == 2)
        val rd = oplist(0).asRegister
        if (opcode.rawcode != "MOV") {
          val imm = oplist(1).asImmediate
          if (imm.sizeInBits == 0)
            MoveInst(addr, opcode, rd, imm.resize(16))
          else
            MoveInst(addr, opcode, rd, imm)
        }
        else
          MoveInst(addr, opcode, rd, oplist(1))
      case EXT =>
        require(oplist.length == 2)
        require(oplist(0).isRegister && oplist(1).isRegister)
        ExtensionInst(addr, opcode, oplist(0).asRegister, oplist(1).asRegister)
      case BFM => // FIXME: Interpretation of this opcode is incomplete
        require(oplist.length == 4)
        require(oplist(0).isRegister && oplist(1).isRegister 
            && oplist(2).isImmediate && oplist(3).isImmediate)
        BitfieldMoveInst(addr, opcode, oplist(0).asRegister, 
            oplist(1).asRegister, oplist(2).asImmediate, oplist(3).asImmediate)
      case DataProcess =>
        require(oplist.length == 2)
        DataProcessInst(addr, opcode, oplist(0).asRegister, oplist(1).asRegister)
      case Branch =>
        require(oplist.length <= 1)
        if (oplist.length == 1) {
          require(!oplist(0).isImmediate)
          BranchInst(addr, opcode, oplist(0))
        } else {
          BranchInst(addr, opcode, Register.get("X30"))
        }
      case CompBranch =>
        require(oplist.length == 2)
        require(oplist(0).isRegister && oplist(1).isMemory)
        CompBranchInst(addr, opcode, oplist(0).asRegister, oplist(1).asMemory)
      case TestBranch =>
        require(oplist.length == 3)
        require(oplist(0).isRegister && oplist(1).isImmediate && oplist(2).isMemory)
        TestBranchInst(addr, opcode, oplist(0).asRegister, oplist(1).asImmediate, oplist(1).asMemory)
      case System => SystemInst(addr, opcode, oplist)
      case Nop => NopInst(addr, opcode)
      case Unsupported => Exception.unreachable()
    }
    
  }
  
}

sealed abstract class Instruction(oplist: Operand*)
  extends MachEntry[AArch64] with Iterable[Operand] {
  
  val mach = AArch64Machine
  val addr: Long
  val opcode: Opcode 
  val operands = oplist.toVector
  
  protected final def operand(idx: Int) = operands(idx)
  protected final def numOfOperands = operands.length
  
  override def iterator = operands.iterator
  override val index = addr
  
}

case class TrinaryArithInst(addr: Long, opcode: Opcode, dest: Register,
    src1: Register, src2: Register, src3: Register)
    extends Instruction(dest, src1, src2, src3)

case class BinaryArithInst(addr: Long, opcode: Opcode, dest: Register,
    srcLeft: Register, srcRight: Operand)
    extends Instruction(dest, srcLeft, srcRight) {

  def subtype = Opcode.Mnemonic.BinArith.Subtype.withName(opcode.rawcode)
  def updateFlags = opcode.rawcode.endsWith("S")
  
}

case class UnaryArithInst(addr: Long, opcode: Opcode, dest: Register,
    src: Operand) extends Instruction(dest, src) {
  
  def subtype = Opcode.Mnemonic.UnArith.Subtype.withName(opcode.rawcode)
  def updateFlags = opcode.rawcode.endsWith("S")
  
}

sealed trait Extension

object Extension {
  
  case object Signed extends Extension 
  case object Unsigned extends Extension 
  case object NoExtension extends Extension
  
}

// This is actually a pseudo instruction. In the original Java implementation,
// ExtensionInst is a subclass of BitfieldMoveInst. In Scala, however, it
// is not possible to inherit from a case class
case class ExtensionInst(addr: Long, opcode: Opcode, dest: Register,
    src: Register) extends Instruction(dest, src) {
  
  val extension = {
    val first = opcode.rawcode.charAt(0)
    if (first == 'S')
      Extension.Signed
    else if (first == 'U')
      Extension.Unsigned
    else
      Extension.NoExtension
      
  }
  
  require(extension != Extension.NoExtension)
  
}

case class BitfieldMoveInst(addr: Long, opcode: Opcode, dest: Register,
    src: Register, rotate: Immediate, shift: Immediate)
    extends Instruction(dest, src, rotate, shift) {
  
  val extension = {
    val first = opcode.rawcode.charAt(0)
    if (first == 'S')
      Extension.Signed
    else if (first == 'U')
      Extension.Unsigned
    else
      Extension.NoExtension
  }
  
}

object AbstractBranch {
  import scala.collection.mutable.Map
  import edu.psu.ist.plato.kaiming.utils.RefWrapper
  
  private val _belongs = Map[RefWrapper[BranchInst], MachBBlock[AArch64]]()
  private def loopUpRelocation(b: AbstractBranch) = _belongs.get(new RefWrapper(b))
  private def relocateTarget(b: AbstractBranch, bb: MachBBlock[AArch64]) =
    _belongs += (new RefWrapper(b) -> bb)
}

sealed abstract class AbstractBranch(ops: Operand*)
    extends Instruction(ops: _*) with Terminator[AArch64] {
  
  override def relocate(target: MachBBlock[AArch64]) = 
    AbstractBranch.relocateTarget(this, target)

  override def relocatedTarget = AbstractBranch.loopUpRelocation(this)
  
}

case class TestBranchInst(addr: Long, opcode: Opcode, toTest: Register,
    imm: Immediate, target: Memory) extends AbstractBranch(toTest, imm, target) {
  
  def subtype = Opcode.Mnemonic.TestBranch.Subtype.withName(opcode.rawcode)
  
  override def dependentFlags = Set()
  override val isReturn = false
  override val isCall = false
  override val isIndirect = false
  override val isTargetConcrete = true
  override def targetIndex = 
    target.off match {
      case Some(Left(imm)) => imm.value
      case _ => Exception.unreachable()
    }
  
}

case class CompBranchInst(addr: Long, opcode: Opcode, toCompare: Register,
    target: Memory) extends AbstractBranch(toCompare, target) {
  
  def subtype = Opcode.Mnemonic.CompBranch.Subtype.withName(opcode.rawcode)
  
  override def dependentFlags = Set()
  override val isReturn = false
  override val isCall = false
  override val isIndirect = false
  override val isTargetConcrete = true
  override def targetIndex = 
    target.off match {
      case Some(Left(imm)) => imm.value
      case _ => Exception.unreachable()
    }
  
}

case class BranchInst(addr: Long, opcode: Opcode, target: Operand)
    extends AbstractBranch(target) {
  
  val subtype = Opcode.Mnemonic.Branch.Subtype.withName(opcode.rawcode.split("\\.")(0))
  import Opcode.Mnemonic.Branch.Subtype._
  
  def condition = opcode.getCondition
  def hasLink = subtype match {
    case BL | BLR => true
    case B | RET | BR => false
  }
  
  override def dependentFlags = condition.dependentMachFlags
  override val isReturn = target.isRegister && target.asRegister.id == Register.Id.X30
  override val isCall = hasLink
  override val isIndirect = target.isRegister
  override val isTargetConcrete = target.isMemory
  override def targetIndex = 
    if (target.isRegister)
      throw new UnsupportedOperationException()
    else
      target.asMemory.off match {
        case Some(Left(imm)) => imm.value
        case _ => Exception.unreachable()
      }


}

case class MoveInst(addr: Long, opcode: Opcode,
    dest: Register, src: Operand) extends Instruction(dest, src) {
  
  def subtype = Opcode.Mnemonic.Move.Subtype.withName(opcode.rawcode)
  
}

case class CompareInst(addr: Long, opcode: Opcode,  left: Register,
    right: Operand) extends Instruction(left, right) {
  
  def subtype = Opcode.Mnemonic.Compare.Subtype.withName(opcode.rawcode)
  
}

case class CondCompareInst(addr: Long, opcode: Opcode, left: Register,
    right: Operand, nzcv: Int, cond: Condition) extends Instruction(left, right) {
  
  def subtype = Opcode.Mnemonic.CondCompare.Subtype.withName(opcode.rawcode)
  
}

case class PCRelativeInst(addr: Long, opcode: Opcode, dest: Register, ptr: Immediate)
    extends Instruction(dest, ptr)

case class DataProcessInst(addr: Long, opcode: Opcode, dest: Register, src: Register)
    extends Instruction(dest, src) {
  
  def subtype = Opcode.Mnemonic.DataProcess.Subtype.withName(opcode.rawcode)
  
}
  
sealed abstract class LoadStoreInst(val addressingMode: AddressingMode,
    oplist: Operand*) extends Instruction(oplist:_*) {
  
  def indexingOperandIndex: Int
  def indexingOperand = operand(indexingOperandIndex).asMemory
  
}

case class LoadInst(addr: Long, opcode: Opcode,
    dest: Register, mem: Memory, mode: AddressingMode)
    extends LoadStoreInst(mode, dest, mem) {
  
  override def indexingOperandIndex = 1
  
}

case class LoadPairInst(addr: Long, opcode: Opcode, destLeft: Register,
    destRight: Register, mem: Memory, mode: AddressingMode)
    extends LoadStoreInst(mode, destLeft, destRight, mem) {
  
  override def indexingOperandIndex = 2

}

case class StoreInst(addr: Long, opcode: Opcode, src: Register, mem: Memory,
    mode: AddressingMode) extends LoadStoreInst(mode, src, mem) {
  
  override def indexingOperandIndex = 1
  
}

case class StorePairInst(addr: Long, opcode: Opcode, srcLeft: Register,
    srcRight: Register, mem: Memory, mode: AddressingMode)
    extends LoadStoreInst(mode, srcLeft, srcRight, mem) {
  
  override def indexingOperandIndex = 2

}

case class StoreExclusiveInst(addr: Long, opcode: Opcode, result: Register,
    src: Register, mem: Memory)
    extends LoadStoreInst(AddressingMode.Regular, result, src, mem) {
  
  override def indexingOperandIndex = 2
  
}

case class StorePairExclusiveInst(addr: Long, opcode: Opcode, result: Register,
    srcLeft: Register, srcRight: Register, mem: Memory)
    extends LoadStoreInst(AddressingMode.Regular, result, srcLeft, srcRight, mem) {
  
  override def indexingOperandIndex = 3
  
}

sealed abstract class SelectInst(regs: Register*) extends Instruction(regs: _*) {
  val dest: Register
  val condition: Condition
}

case class UnarySelectInst(addr: Long, opcode: Opcode, dest: Register,
    condition: Condition) extends SelectInst(dest) {
  
  def subtype = Opcode.Mnemonic.UnSel.Subtype.withName(opcode.rawcode)
  
}

case class BinarySelectInst(addr: Long, opcode: Opcode, dest: Register,
    src: Register, condition: Condition) extends SelectInst(dest) {
  
  def subtype = Opcode.Mnemonic.BinSel.Subtype.withName(opcode.rawcode)
  
}

case class TrinarySelectInst(addr: Long, opcode: Opcode, dest: Register,
    srcTrue: Register, srcFalse: Register, condition: Condition)
    extends SelectInst(dest) {
  
  def subtype = Opcode.Mnemonic.TriSel.Subtype.withName(opcode.rawcode)
  
}

case class SystemInst(addr: Long, opcode: Opcode,
    override val operands: Vector[Operand]) extends Instruction(operands: _*)

case class NopInst(addr: Long, opcode: Opcode) extends Instruction()
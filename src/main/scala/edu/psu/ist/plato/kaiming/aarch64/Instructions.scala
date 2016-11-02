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
  
  def create(addr: Long, opcode: Opcode, oplist: Vector[Operand],
      cond: Option[Condition], preidx: Boolean): Instruction = {
    val condition = cond.getOrElse(Condition.AL)
    import edu.psu.ist.plato.kaiming.aarch64.Opcode.OpClass._
    opcode.mnemonic match {
      case BinArith => 
        require((oplist.length == 3 && oplist(1).isRegister) || oplist.length == 2)
        val rd = oplist(0).asRegister
        if (oplist.length == 3)
          BinaryArithInst(addr, opcode.rawcode, rd, oplist(1).asRegister, oplist(2))
        else
          BinaryArithInst(addr, opcode.rawcode, rd, rd, oplist(1))
      case PCRelative =>
        require(oplist.length == 2)
        PCRelativeInst(addr, opcode.rawcode, oplist(0).asRegister, oplist(1).asImmediate)
      case UnArith =>
        require(oplist.length == 2 && !oplist(1).isMemory)
        UnaryArithInst(addr, opcode.rawcode, oplist(0).asRegister, oplist(1))
      case TriArith =>
        require(oplist.length == 4)
        TrinaryArithInst(addr, opcode.rawcode, oplist(0).asRegister,
            oplist(1).asRegister, oplist(2).asRegister, oplist(3).asRegister)
      case Load | Store => {
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
        if (opcode.mnemonic == Load)
          LoadInst(addr, opcode.rawcode, rd, mem, mode)
        else
          StoreInst(addr, opcode.rawcode, rd, mem, mode)
      }
      case LoadPair | StorePair => {
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
        if (opcode.mnemonic == LoadPair)
          LoadPairInst(addr, opcode.rawcode, rd1, rd2, mem, mode)
        else
          StorePairInst(addr, opcode.rawcode, rd1, rd2, mem, mode)
      }
      case StoreEx =>
        require(oplist.length == 3)
        StoreExclusiveInst(addr, opcode.rawcode, oplist(0).asRegister,
            oplist(1).asRegister, oplist(2).asMemory)
      case StoreExPair =>
        require(oplist.length == 4)
        StorePairExclusiveInst(addr, opcode.rawcode, oplist(0).asRegister,
            oplist(1).asRegister, oplist(2).asRegister, oplist(3).asMemory)
      case Compare =>
        require(oplist.length == 2 && !oplist(1).isMemory)
        val rd = oplist(0).asRegister
        CompareInst(addr, opcode.rawcode, rd, oplist(1))
      case CondCompare =>
        require(oplist.length == 3 && cond.isDefined && !oplist(1).isMemory)
        val rd = oplist(0).asRegister
        CondCompareInst(addr, opcode.rawcode, rd, oplist(1),
            oplist(2).asImmediate, condition)
      case UnSel =>
        require(oplist.length == 1)
        UnarySelectInst(addr, opcode.rawcode, oplist(0).asRegister, condition)
      case BinSel =>
        require(oplist.length == 2)
        BinarySelectInst(addr, opcode.rawcode, oplist(0).asRegister,
            oplist(1).asRegister, condition)
      case TriSel =>
        require(oplist.length == 3)
        TrinarySelectInst(addr, opcode.rawcode, oplist(0).asRegister,
            oplist(1).asRegister, oplist(2).asRegister, condition)
      case Move =>
        require(oplist.length == 2)
        MoveInst(addr, opcode.rawcode, oplist(0).asRegister, oplist(1))
      case Extend =>
        require(oplist.length == 2)
        require(oplist(0).isRegister && oplist(1).isRegister)
        ExtendInst(addr, opcode.rawcode, oplist(0).asRegister, oplist(1).asRegister)
      case BFMove =>
        require(oplist.length == 4)
        BitfieldMoveInst(addr, opcode.rawcode, oplist(0).asRegister, 
            oplist(1).asRegister, oplist(2).asImmediate, oplist(3).asImmediate)
      case DataProcess =>
        require(oplist.length == 2)
        DataProcessInst(addr, opcode.rawcode, oplist(0).asRegister, oplist(1).asRegister)
      case Branch =>
        require(oplist.length <= 1)
        if (oplist.length == 1) {
            require(!oplist(0).isImmediate, addr.toHexString)
            BranchInst(addr, opcode.rawcode, oplist(0))
        } else {
          BranchInst(addr, opcode.rawcode, Register.get(Register.Id.X30))
        }
      case CompBranch =>
        require(oplist.length == 2)
        CompBranchInst(addr, opcode.rawcode, oplist(0).asRegister, oplist(1).asMemory)
      case TestBranch =>
        require(oplist.length == 3)
        val r = oplist(0).asRegister
        TestBranchInst(addr, opcode.rawcode, r, oplist(1).asImmediate, oplist(2).asMemory)
      case System => SystemInst(addr, opcode.rawcode, oplist)
      case Nop => NopInst(addr)
      case Unsupported => Exception.unreachable()
    }
    
  }
  
}

sealed abstract class Instruction(oplist: Operand*)
  extends MachEntry[AArch64] with Iterable[Operand] {
  
  val mach = AArch64Machine
  val addr: Long
  val mnem: String 
  val operands = oplist.toVector
  
  protected final def operand(idx: Int) = operands(idx)
  protected final def numOfOperands = operands.length
  
  override def iterator = operands.iterator
  override val index = addr
  
}

case class TrinaryArithInst protected (addr: Long, mnem: String, dest: Register,
    src1: Register, src2: Register, src3: Register)
    extends Instruction(dest, src1, src2, src3) {
  
  def subtype = Opcode.OpClass.TriArith.Mnemonic.withName(mnem)
  
}

case class BinaryArithInst(addr: Long, mnem: String, dest: Register,
    srcLeft: Register, srcRight: Operand)
    extends Instruction(dest, srcLeft, srcRight) {

  def subtype = Opcode.OpClass.BinArith.Mnemonic.withName(mnem)
  def updateFlags = mnem.endsWith("S")
  
}

case class UnaryArithInst(addr: Long, mnem: String, dest: Register,
    src: Operand) extends Instruction(dest, src) {
  
  def subtype = Opcode.OpClass.UnArith.Mnemonic.withName(mnem)
  def updateFlags = mnem.endsWith("S")
  
}

sealed trait Extension

object Extension {
  
  case object Signed extends Extension 
  case object Unsigned extends Extension 
  
}

// This is actually a pseudo instruction. In the original Java implementation,
// ExtensionInst is a subclass of BitfieldMoveInst. In Scala, however, it
// is not possible to inherit from a case class
case class ExtendInst(addr: Long, mnem: String, dest: Register,
    src: Register) extends Instruction(dest, src) {
  
  def subtype = Opcode.OpClass.Extend.Mnemonic.withName(mnem)
  import Opcode.OpClass.Extend.Mnemonic._
  
  val (extension, width) = subtype match {
    case SXTB => (Extension.Signed, 8)
    case SXTH => (Extension.Signed, 16)
    case SXTW => (Extension.Signed, 32)
    case UXTB => (Extension.Unsigned, 8)
    case UXTH => (Extension.Unsigned, 16)
    case UXTW => (Extension.Unsigned, 32)
  }
  
}

case class BitfieldMoveInst(addr: Long, mnem: String, dest: Register,
    src: Register, imm1: Immediate, imm2: Immediate)
    extends Instruction(dest, src, imm1, imm2) {
  
  def subtype = Opcode.OpClass.BFMove.Mnemonic.withName(mnem)
  import Opcode.OpClass.BFMove.Mnemonic._
  
  val extension: Option[Extension] = subtype match {
    case BFM | BFI | BFXIL => None
    case SBFM | SBFIZ | SBFX => Some(Extension.Signed)
    case UBFM | UBFIZ | UBFX => Some(Extension.Unsigned)
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

case class TestBranchInst(addr: Long, mnem: String, toTest: Register,
    imm: Immediate, target: Memory) extends AbstractBranch(toTest, imm, target) {
  
  def subtype = Opcode.OpClass.TestBranch.Mnemonic.withName(mnem)
  
  override def isReturn = false
  override def isCall = false
  override def isIndirect = false
  override def isTargetConcrete = true
  override def isConditional = true
  override def targetIndex = 
    target.off match {
      case Some(Left(imm)) => imm.value
      case _ => Exception.unreachable()
    }
  
}

case class CompBranchInst(addr: Long, mnem: String, toCompare: Register,
    target: Memory) extends AbstractBranch(toCompare, target) {
  
  def subtype = Opcode.OpClass.CompBranch.Mnemonic.withName(mnem)
  
  override def isConditional = true
  override def isReturn = false
  override def isCall = false
  override def isIndirect = false
  override def isTargetConcrete = true
  override def targetIndex = 
    target.off match {
      case Some(Left(imm)) => imm.value
      case _ => Exception.unreachable()
    }
  
}

case class BranchInst(addr: Long, mnem: String, target: Operand)
    extends AbstractBranch(target) {
  
  val subtype = Opcode.OpClass.Branch.Mnemonic.withName(mnem.split("\\.")(0))
  import Opcode.OpClass.Branch.Mnemonic._
  
  def condition = {
    val parts = mnem.split("\\.")
    assert(parts.length <= 2)
    if (parts.length == 2) {
      assert(parts(0).equals("B"))
      Condition.withName(parts(1))
    } else {
      Condition.AL
    }
  }
  
  def hasLink = subtype match {
    case BL | BLR => true
    case B | RET | BR => false
  }
  
  override def isConditional = condition != Condition.AL
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

case class MoveInst(addr: Long, mnem: String,
    dest: Register, src: Operand) extends Instruction(dest, src) {
  
  def subtype = Opcode.OpClass.Move.Mnemonic.withName(mnem)
  
}

case class CompareInst(addr: Long, mnem: String,  left: Register,
    right: Operand) extends Instruction(left, right) {
  
  def subtype = Opcode.OpClass.Compare.Mnemonic.withName(mnem)
  
}

case class CondCompareInst(addr: Long, mnem: String, left: Register,
    right: Operand, nzcv: Immediate, condition: Condition) extends Instruction(left, right, nzcv) {
  
  def subtype = Opcode.OpClass.CondCompare.Mnemonic.withName(mnem)
  
}

case class PCRelativeInst(addr: Long, mnem: String, dest: Register, ptr: Immediate)
    extends Instruction(dest, ptr)

case class DataProcessInst(addr: Long, mnem: String, dest: Register, src: Register)
    extends Instruction(dest, src) {
  
  def subtype = Opcode.OpClass.DataProcess.Mnemonic.withName(mnem)
  
}
  
sealed abstract class LoadStoreInst(val addressingMode: AddressingMode,
    oplist: Operand*) extends Instruction(oplist:_*) {
  
  def indexingOperandIndex: Int
  def indexingOperand = operand(indexingOperandIndex).asMemory
  
}

case class LoadInst(addr: Long, mnem: String,
    dest: Register, mem: Memory, mode: AddressingMode)
    extends LoadStoreInst(mode, dest, mem) {
  
  override def indexingOperandIndex = 1
  
}

case class LoadPairInst(addr: Long, mnem: String, destLeft: Register,
    destRight: Register, mem: Memory, mode: AddressingMode)
    extends LoadStoreInst(mode, destLeft, destRight, mem) {
  
  override def indexingOperandIndex = 2

}

case class StoreInst(addr: Long, mnem: String, src: Register, mem: Memory,
    mode: AddressingMode) extends LoadStoreInst(mode, src, mem) {
  
  override def indexingOperandIndex = 1
  
}

case class StorePairInst(addr: Long, mnem: String, srcLeft: Register,
    srcRight: Register, mem: Memory, mode: AddressingMode)
    extends LoadStoreInst(mode, srcLeft, srcRight, mem) {
  
  override def indexingOperandIndex = 2

}

case class StoreExclusiveInst(addr: Long, mnem: String, result: Register,
    src: Register, mem: Memory)
    extends LoadStoreInst(AddressingMode.Regular, result, src, mem) {
  
  override def indexingOperandIndex = 2
  
}

case class StorePairExclusiveInst(addr: Long, mnem: String, result: Register,
    srcLeft: Register, srcRight: Register, mem: Memory)
    extends LoadStoreInst(AddressingMode.Regular, result, srcLeft, srcRight, mem) {
  
  override def indexingOperandIndex = 3
  
}

sealed abstract class SelectInst(regs: Register*) extends Instruction(regs: _*) {
  val dest: Register
  val condition: Condition
}

case class UnarySelectInst(addr: Long, mnem: String, dest: Register,
    condition: Condition) extends SelectInst(dest) {
  
  def subtype = Opcode.OpClass.UnSel.Mnemonic.withName(mnem)
  
}

case class BinarySelectInst(addr: Long, mnem: String, dest: Register,
    src: Register, condition: Condition) extends SelectInst(dest) {
  
  def subtype = Opcode.OpClass.BinSel.Mnemonic.withName(mnem)
  
}

case class TrinarySelectInst(addr: Long, mnem: String, dest: Register,
    srcTrue: Register, srcFalse: Register, condition: Condition)
    extends SelectInst(dest) {
  
  def subtype = Opcode.OpClass.TriSel.Mnemonic.withName(mnem)
  
}

case class SystemInst(addr: Long, mnem: String,
    override val operands: Vector[Operand]) extends Instruction(operands: _*)

case class NopInst(addr: Long) extends Instruction() {
  val mnem = "NOP"
}
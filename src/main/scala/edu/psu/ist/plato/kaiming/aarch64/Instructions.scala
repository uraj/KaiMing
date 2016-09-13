package edu.psu.ist.plato.kaiming.aarch64

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.AArch64
import edu.psu.ist.plato.kaiming.exception.UnreachableCodeException

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
      case ADD | SUB | MUL | SDIV | UDIV | ASR | LSL | LSR | ORR | ORN | AND => 
        require(oplist(0).isRegister)
        require((oplist.length == 3 && oplist(1).isRegister) || oplist.length == 2)
        val rd = oplist(0).asRegister
        if (oplist.length == 3)
          BinaryArithInst(addr, opcode, rd, oplist(1).asRegister, 
              resizeIfImm(oplist(2), rd.sizeInBits))
        else
          BinaryArithInst(addr, opcode, rd, rd, resizeIfImm(oplist(1), rd.sizeInBits))
      case ADR | NEG =>
        require(oplist.length == 2)
        require(oplist(0).isRegister && (oplist(1).isRegister || oplist(1).isImmediate))
        UnaryArithInst(addr, opcode, oplist(0).asRegister, oplist(1))
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
      case LDP | STP => {
        require(oplist.length == 3 || oplist.length == 4)
        require(oplist(0).isRegister && oplist(1).isRegister && oplist(2).isMemory)
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
      case TST | CMP | CMN =>
        require(oplist.length == 2)
        require(oplist(0).isRegister && (oplist(1).isRegister || oplist(1).isImmediate))
        val rd = oplist(0).asRegister
        CompareInst(addr, opcode, rd, resizeIfImm(oplist(1), rd.sizeInBits))
      case CSEL | CSINC =>
        require(oplist.length == 3)
        require(oplist(0).isRegister && oplist(1).isRegister && oplist(2).isRegister)
        SelectInst(addr, opcode, oplist(0).asRegister, 
            oplist(1).asRegister, oplist(2).asRegister, condition)
      case CINC =>
        require(oplist.length == 2)
        require(oplist(0).isRegister && oplist(1).isRegister)
        SelectInst(addr, Opcode("CSINC"), oplist(0).asRegister, oplist(1).asRegister,
            oplist(1).asRegister, condition.invert)
      case CSET => {
        require(oplist.length == 1)
        require(oplist(0).isRegister)
        val rd = oplist(0).asRegister
        val zero = if(rd.sizeInBits == 32) Register.get("WZR") else Register.get("XZR")
        SelectInst(addr, Opcode("CSINC"), oplist(0).asRegister, zero, zero, condition.invert)
      }
      case MOV | MOVK =>
        require(oplist.length == 2)
        require(oplist(0).isRegister)
        MoveInst(addr, opcode, oplist(0).asRegister, oplist(1))
      case EXT =>
        require(oplist.length == 2)
        require(oplist(0).isRegister && oplist(1).isRegister)
        ExtensionInst(addr, opcode, oplist(0).asRegister, oplist(1).asRegister)
      case BFM =>
        require(oplist.length == 4)
        require(oplist(0).isRegister && oplist(1).isRegister 
            && oplist(2).isImmediate && oplist(3).isImmediate)
        BitfieldMoveInst(addr, opcode, oplist(0).asRegister, 
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
  extends MachEntry[AArch64] with Iterable[Operand] {
  
  val mach = AArch64Machine
  
  val operands = oplist.toVector
  val addr: Long
  val opcode: Opcode
  
  protected final def operand(idx: Int) = operands(idx)
  protected final def numOfOperands = operands.length
  
  // This method should not be used inside Instruction and its subclasses.
  // By override this method, specific instructions can ``hide'' certain 
  // operands from outside. This is useful for implementing alias instructions
  // which are frequently seen in ARM64
  override def iterator = operands.iterator
    
  override val index = addr
  
}

case class BinaryArithInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, srcLeft: Register, srcRight: Operand)
  extends Instruction(dest, srcLeft, srcRight) {
  
  def updateFlags = opcode.rawcode.endsWith("S")
  
}

case class UnaryArithInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, src: Operand) extends Instruction(dest, src) {
  
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
case class ExtensionInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, src: Register) extends Instruction(dest, src) {
  
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

case class BitfieldMoveInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, src: Register, rotate: Immediate, shift: Immediate)
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

object BranchInst {
  import scala.collection.mutable.Map
  import edu.psu.ist.plato.kaiming.utils.RefWrapper
  
  private val _belongs = Map[RefWrapper[BranchInst], MachBBlock[AArch64]]()
  private def loopUpRelocation(b: BranchInst) = _belongs.get(new RefWrapper(b))
  private def relocateTarget(b: BranchInst, bb: MachBBlock[AArch64]) =
    _belongs += (new RefWrapper(b) -> bb)
}

case class BranchInst(override val addr: Long, override val opcode: Opcode,
    target: Operand) extends Instruction(target) with Terminator[AArch64] {
  
  val condition = opcode.getCondition()
  val hasLink = opcode.mnemonic == Opcode.Mnemonic.BL
  def dependentFlags = condition.dependentMachFlags
  
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
        case _ => throw new UnsupportedOperationException()
    }
  override def relocate(target: MachBBlock[AArch64]) = 
    BranchInst.relocateTarget(this, target)

  override def relocatedTarget = BranchInst.loopUpRelocation(this)
}

case class MoveInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, src: Operand) extends Instruction(dest, src) {
  
  val doesKeep = opcode.mnemonic == Opcode.Mnemonic.MOVK
  
}

sealed trait CompareCode
case object Compare extends CompareCode
case object Test extends CompareCode
case object CompareNeg extends CompareCode

case class CompareInst(override val addr: Long, override val opcode: Opcode,
    left: Register, right: Operand) extends Instruction(left, right) {
  val code = opcode.mnemonic match {
    case Opcode.Mnemonic.CMP => Compare
    case Opcode.Mnemonic.CMN => CompareNeg
    case Opcode.Mnemonic.TST => Test
    case _ => throw new UnreachableCodeException()
  }
}

sealed abstract class LoadStoreInst(val addressingMode: AddressingMode,
    oplist: Operand*) extends Instruction(oplist:_*) {
  
  def indexingOperandIndex: Int
  def indexingOperand = operand(indexingOperandIndex).asMemory
  
}

case class LoadInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, mem: Memory, mode: AddressingMode)
    extends LoadStoreInst(mode, dest, mem) {
  
  override val indexingOperandIndex = 1
  
}

case class LoadPairInst(override val addr: Long, override val opcode: Opcode,
    destLeft: Register, destRight: Register, mem: Memory,
    mode: AddressingMode) extends LoadStoreInst(mode, destLeft, destRight, mem) {
  
  override val indexingOperandIndex = 2

}

case class StoreInst(override val addr: Long, override val opcode: Opcode,
    src: Register, mem: Memory, mode: AddressingMode)
    extends LoadStoreInst(mode, src, mem) {
  
  override val indexingOperandIndex = 1
  
}

case class StorePairInst(override val addr: Long, override val opcode: Opcode,
    srcLeft: Register, srcRight: Register, mem: Memory,
    mode: AddressingMode) extends LoadStoreInst(mode, srcLeft, srcRight, mem) {
  
  override val indexingOperandIndex = 2

}

case class SelectInst(override val addr: Long, override val opcode: Opcode,
    dest: Register, srcTrue: Register, srcFalse: Register, condition: Condition)
    extends Instruction(dest, srcTrue, srcFalse) {
  
  val doesIncrementSecond = opcode.mnemonic == Opcode.Mnemonic.CSINC
  
}

case class NopInst(override val addr: Long, override val opcode: Opcode)
    extends Instruction()


package edu.psu.ist.plato.kaiming.aarch64

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.AArch64
import edu.psu.ist.plato.kaiming.Arch.KaiMing

import edu.psu.ist.plato.kaiming.ir._

import edu.psu.ist.plato.kaiming.exception.UnreachableCodeException

object AArch64Machine extends Machine[AArch64] {
  
  override val returnRegister = Register.get(Register.Id.X0, None)
  override val wordSizeInBits = 64
  override val registers = 
    Register.Id.values.map(rid => Register.get(rid, None))
    .toSet[MachRegister[AArch64]]
  
  import scala.language.implicitConversions
  
  private implicit def toExpr(imm: Immediate): Const = Const(imm.value)
  
  private implicit def toExpr(reg: Register): Expr = {
    val ret = Reg(reg)
    reg.shift match {
      case None => ret
      case Some(shift) => shift match {
        case Asr(v) => ret.sar(Const(v))
        case Lsl(v) => ret.shl(Const(v))
        case Ror(v) => ret.ror(Const(v))
      }
    }
  }
  
  private implicit def toExpr(mem: Memory): Expr = {
    val ret: Option[Expr] = mem.base.map { x => x }
    mem.off match {
      case None => ret.get
      case Some(off) => {
        val oe: Expr = off match {
          case Left(imm) => imm
          case Right(reg) => reg
        }
        ret match {
          case Some(expr) => expr.add(oe)
          case None => oe
        }
      }
    }
  }
  
  private implicit def operandToExpr(op : Operand): Expr = {
    op match {
      case imm: Immediate => imm
      case reg: Register => reg
      case mem: Memory => mem
    }
  }
  
  private implicit def toExpr(cond: Condition) = cond match {
    case Condition.AL | Condition.NV => Const(1)
    case Condition.EQ => Flg(Flag.Z)
    case Condition.GE => Flg(Flag.N).sub(Flg(Flag.V)).not
    case Condition.GT => Flg(Flag.Z).not.and(Flg(Flag.N).sub(Flg(Flag.V).not))
    case Condition.HI => Flg(Flag.C).and(Flg(Flag.Z).not)
    case Condition.HS => Flg(Flag.C)
    case Condition.LE => Flg(Flag.Z).or(Flg(Flag.N).sub(Flg(Flag.Z)))
    case Condition.LO => Flg(Flag.C).not
    case Condition.LS => Flg(Flag.Z).or(Flg(Flag.C).not)
    case Condition.LT => Flg(Flag.N).sub(Flg(Flag.V))
    case Condition.MI => Flg(Flag.N)
    case Condition.NE => Flg(Flag.Z).not
    case Condition.PL => Flg(Flag.N).not
    case Condition.VC => Flg(Flag.V).not
    case Condition.VS => Flg(Flag.V)
  }
    
  private def updateFlags(inst: Instruction, ce: CompoundExpr,
      builder: IRBuilder) = {
    val b1 = builder + SetFlgStmt(builder.nextIndex, inst, Extractor.Carry, Flg(Flag.C), ce)
    val b2 = b1 + SetFlgStmt(b1.nextIndex, inst, Extractor.Negative, Flg(Flag.N), ce)
    val b3 = b2 + SetFlgStmt(b2.nextIndex, inst, Extractor.Zero, Flg(Flag.Z), ce) 
    b3 + SetFlgStmt(b3.nextIndex, inst, Extractor.Overflow, Flg(Flag.V), ce)
  }
  
  private def toIR(inst: BinaryArithInst, builder: IRBuilder) = {
    val lval = Reg(inst.dest.asRegister)
    import edu.psu.ist.plato.kaiming.aarch64.Opcode.Mnemonic._
    val rval = inst.opcode.mnemonic match {
      case ADD => {
        val add = inst.srcLeft.add(inst.srcRight)
        if (inst.opcode.rawcode.charAt(2) == 'C')
          add.add(Flg(Flag.C))
        else
          add
      }
      case SUB => {
        val sub = inst.srcLeft.sub(inst.srcRight)
        if (inst.opcode.rawcode.charAt(2) == 'C')
          sub.sub(Flg(Flag.C))
        else
          sub
      }
      case MUL => inst.srcLeft.mul(inst.srcRight)
      case DIV => inst.srcLeft.div(inst.srcRight)
      case ASR => inst.srcLeft.sar(inst.srcRight)
      case LSL => inst.srcLeft.shl(inst.srcRight)
      case LSR => inst.srcLeft.shr(inst.srcRight)
      case ORR => inst.srcLeft.or(inst.srcRight)
      case ORN => inst.srcLeft.or(inst.srcRight).not
      case AND => inst.srcLeft.and(inst.srcRight)
      case _ => throw new UnreachableCodeException()
    }
    val nbuilder = builder + AssignStmt(builder.nextIndex, inst, lval, rval)
    if (inst.updateFlags)
      updateFlags(inst, rval, nbuilder)
    else
      nbuilder
  }
  
  private def toIR(inst: ExtensionInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    val e = inst.extension match {
      case Extension.Signed => lv.sext(Const(lv.sizeInBits))
      case _ => lv.uext(Const(lv.sizeInBits))
    }
    builder + AssignStmt(builder.nextIndex, inst, lv, e)
  }
  
  private def toIR(ctx: Context, inst: BitfieldMoveInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    val rotate = inst.rotate.value.toInt
    val shift = inst.shift.value.toInt
    val size = lv.sizeInBits
    val (destInsig, destSig, srcInsig, srcSig) =
      if (shift >= rotate) (0, shift - rotate, rotate, shift)
      else (size - rotate, size + shift - rotate, 0, shift)
    val assigned: Expr = if (srcInsig > 0) inst.src.shr(Const(srcInsig)) else inst.src
    val tmp = ctx.getNewTempVar(srcSig - srcInsig + 1)
    val b = builder + AssignStmt(builder.nextIndex, inst, tmp, Const(0))
    val (assigned2, b2) =
      if (srcInsig > 0) {
        val tmp2 = ctx.getNewTempVar(srcInsig)
        (tmp.concat(tmp2), b + AssignStmt(b.nextIndex, inst, tmp2, Const(0))) 
      } else {
        (tmp, b) 
      }
    val (assigned3, assignRange) = inst.extension match {
      case Extension.Signed => (assigned2.sext(Const(size)), (0, size))
      case Extension.Unsigned => (assigned2.uext(Const(size)), (0, size))
      case Extension.NoExtension => (assigned2, (destInsig, destSig)) 
    }
    b2 + AssignStmt(b2.nextIndex, inst, lv, assigned3, assignRange)
  }
  
  private def toIR(inst: MoveInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    val boundSig = if (inst.doesKeep) 16 else lv.sizeInBits
    builder + AssignStmt(builder.nextIndex, inst, lv, inst.src, (0, boundSig))
  }
  
  private def toIR(inst: CompareInst, builder: IRBuilder) = {
    val cmp = inst.code match {
      case Test => inst.left.and(inst.right)
      case Compare => inst.left.sub(inst.right)
      case CompareNeg => inst.left.add(inst.right)
    }
    updateFlags(inst, cmp, builder)
  }
  
  private def toIR(inst: BranchInst, builder: IRBuilder) = {
    builder + {
      if (inst.isReturn)
        RetStmt(builder.nextIndex, inst, inst.target)
      else if (inst.isCall)
        CallStmt(builder.nextIndex, inst, inst.target)
      else
        JmpStmt(builder.nextIndex, inst, inst.target)
    }
  }
  
  private def toIR(ctx: Context, inst: LoadStoreInst, builder: IRBuilder) = {
    import AddressingMode._
    val addr: Expr = inst.addressingMode match {
      case PostIndex => inst.indexingOperand.base.get
      case PreIndex | Regular => inst.indexingOperand
    }
    val b = inst match {
      case l: LoadInst => processLoadStore(l, addr, builder)
      case l: LoadPairInst => processLoadStore(l, addr, builder)
      case l: StoreInst => processLoadStore(l, addr, builder)
      case l: StorePairInst => processLoadStore(l, addr, builder)
    }
    if (inst.addressingMode != AddressingMode.Regular)
      b + AssignStmt(b.nextIndex, inst, Reg(inst.indexingOperand.base.get), addr)
    else
      b
  }
  
  private def processLoadStore(inst: LoadInst, addr: Expr, builder: IRBuilder) = {
    builder + LdStmt(builder.nextIndex, inst, Reg(inst.dest), addr)
  }
  
  private def processLoadStore(inst: LoadPairInst, addr: Expr, builder: IRBuilder) = {
    val first = Reg(inst.destLeft)
    val b = builder + LdStmt(builder.nextIndex, inst, first, addr)
    val second = Reg(inst.destRight)
    val sizeInBytes = first.sizeInBits / 8
    b + LdStmt(b.nextIndex, inst, second, addr.add(Const(sizeInBytes)))
  }
  
  private def processLoadStore(inst: StoreInst, addr: Expr, builder: IRBuilder) = {
    builder + StStmt(builder.nextIndex, inst, addr, inst.src)
  }
  
  private def processLoadStore(inst: StorePairInst, addr: Expr, builder: IRBuilder) = {
    val first = Reg(inst.srcLeft)
    val b = builder + StStmt(builder.nextIndex, inst, first, addr)
    val second = Reg(inst.srcRight)
    val sizeInBytes = first.sizeInBits / 8
    b + StStmt(b.nextIndex, inst, second, addr.add(Const(sizeInBytes)))
  }
  
  private def toIR(inst: UnaryArithInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    import edu.psu.ist.plato.kaiming.aarch64.Opcode.Mnemonic._
    builder + {
      inst.opcode.mnemonic match {
        case ADR => AssignStmt(builder.nextIndex, inst, lv, inst.src)
        case NEG => AssignStmt(builder.nextIndex, inst, lv, Const(0).sub(inst.src))
        case _ => throw new UnreachableCodeException()
      }
    }
  }
  
  private def toIR(inst: SelectInst, builder: IRBuilder) = {
    builder + SelStmt(builder.nextIndex, inst, Reg(inst.dest),
        inst.condition, inst.srcTrue, inst.srcFalse)
  }
  
  override protected def toIRStatements(ctx: Context, inst: MachEntry[AArch64],
      builder: IRBuilder) = {
    inst.asInstanceOf[Instruction] match {
      case i: BinaryArithInst => toIR(i, builder)
      case i: UnaryArithInst => toIR(i, builder)
      case i: BitfieldMoveInst => toIR(ctx, i, builder)
      case i: ExtensionInst => toIR(i, builder)
      case i: BranchInst => toIR(i, builder)
      case i: CompareInst => toIR(i, builder)
      case i: MoveInst => toIR(i, builder)
      case i: SelectInst => toIR(i, builder)
      case i: LoadStoreInst => toIR(ctx, i, builder)
      case i: NopInst => builder
    }
  }

}
package edu.psu.ist.plato.kaiming.aarch64

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.AArch64
import edu.psu.ist.plato.kaiming.Arch.KaiMing

import edu.psu.ist.plato.kaiming.ir._

import edu.psu.ist.plato.kaiming.exception.UnreachableCodeException

object AArch64Machine extends Machine[AArch64] {
  
  override val returnRegister = Register.get(Register.Id.X0)
  override val wordSizeInBits = 64
  override val registers = 
    Register.Id.values.map(rid => Register.get(rid))
    .toSet[MachRegister[AArch64]]
  
  import scala.language.implicitConversions
  
  private implicit def toExpr(imm: Immediate): Const = Const(imm.value, imm.sizeInBits)
  
  private implicit def toExpr(i: Int): Const = Const(i, wordSizeInBits)
  private implicit def toExpr(i: Long): Const = Const(i, wordSizeInBits)
  
  private implicit def toExpr(sreg: ShiftedRegister): Expr = {
    val ret = Reg(sreg.reg)
    sreg.shift match {
      case None => ret
      case Some(shift) => shift match {
        case Asr(v) => ret >>> v
        case Lsl(v) => ret << v
        case Ror(v) => ret >< v
      }
    }
  }
  
  private implicit def toExpr(reg: Register): Expr = Reg(reg)
  
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
          case Some(expr) => expr + oe
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
      case sreg: ShiftedRegister => sreg
    }
  }
  
  private implicit def toExpr(cond: Condition) = cond match {
    case Condition.AL | Condition.NV => Const(1, 1)
    case Condition.EQ => Flg(Flag.Z)
    case Condition.GE => Flg(Flag.N) - !Flg(Flag.V)
    case Condition.GT => !Flg(Flag.Z) & !(Flg(Flag.N) ^ Flg(Flag.V))
    case Condition.HI => Flg(Flag.C) & !Flg(Flag.Z)
    case Condition.HS => Flg(Flag.C)
    case Condition.LE => Flg(Flag.Z) | (Flg(Flag.N) - Flg(Flag.Z))
    case Condition.LO => !Flg(Flag.C)
    case Condition.LS => Flg(Flag.Z) | !Flg(Flag.C)
    case Condition.LT => Flg(Flag.N) - Flg(Flag.V)
    case Condition.MI => Flg(Flag.N)
    case Condition.NE => !Flg(Flag.Z)
    case Condition.PL => !Flg(Flag.N)
    case Condition.VC => !Flg(Flag.V)
    case Condition.VS => Flg(Flag.V)
  }
    
  private def updateFlags(inst: Instruction, ce: CompoundExpr,
      builder: IRBuilder) = {
    builder.buildSetFlg(inst, Extractor.Carry, Flg(Flag.C), ce)
    .buildSetFlg(inst, Extractor.Negative, Flg(Flag.N), ce)
    .buildSetFlg(inst, Extractor.Zero, Flg(Flag.Z), ce) 
    .buildSetFlg(inst, Extractor.Overflow, Flg(Flag.V), ce)
  }
  
  private def toIR(inst: BinaryArithInst, builder: IRBuilder) = {
    val lval = Reg(inst.dest.asRegister)
    import edu.psu.ist.plato.kaiming.aarch64.Opcode.Mnemonic._
    val rval = inst.opcode.mnemonic match {
      case ADD => {
        val add = inst.srcLeft + inst.srcRight
        if (inst.opcode.rawcode.charAt(2) == 'C')
          add + Flg(Flag.C)
        else
          add
      }
      case SUB => {
        val sub = inst.srcLeft - inst.srcRight
        if (inst.opcode.rawcode.charAt(2) == 'C')
          sub - Flg(Flag.C)
        else
          sub
      }
      case MUL => inst.srcLeft * inst.srcRight
      case DIV => inst.srcLeft / inst.srcRight
      case ASR => inst.srcLeft >>> inst.srcRight
      case LSL => inst.srcLeft << inst.srcRight
      case LSR => inst.srcLeft >> inst.srcRight
      case ORR => inst.srcLeft | inst.srcRight
      case ORN => inst.srcLeft | !inst.srcRight
      case AND => inst.srcLeft & inst.srcRight
      case _ => throw new UnreachableCodeException()
    }
    val nbuilder = builder.buildAssign(inst, lval, rval)
    if (inst.updateFlags)
      updateFlags(inst, rval, nbuilder)
    else
      nbuilder
  }
  
  private def toIR(inst: ExtensionInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    val e = inst.extension match {
      case Extension.Signed => lv sext lv.sizeInBits
      case _ => lv uext lv.sizeInBits
    }
    builder.buildAssign(inst, lv, e)
  }
  
  private def toIR(ctx: Context, inst: BitfieldMoveInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    val rotate = inst.rotate.value.toInt
    val shift = inst.shift.value.toInt
    val size = lv.sizeInBits
    val (destInsig, destSig, srcInsig, srcSig) =
      if (shift >= rotate) (0, shift - rotate, rotate, shift)
      else (size - rotate, size + shift - rotate, 0, shift)
    val assigned: Expr =
      if (srcInsig > 0) inst.src >> Const(srcInsig, inst.src.sizeInBits) else inst.src
    val tmp = ctx.getNewTempVar(srcSig - srcInsig + 1)
    val b = builder.buildAssign(inst, tmp, 0)
    val (assigned2, b2) =
      if (srcInsig > 0) {
        val tmp2 = ctx.getNewTempVar(srcInsig)
        (tmp2 :+ tmp2, b.buildAssign(inst, tmp2, 0)) 
      } else {
        (tmp, b) 
      }
    val assigned3 = inst.extension match {
      case Extension.Signed => assigned2 sext size
      case Extension.Unsigned => assigned2 uext size
      case Extension.NoExtension => (destInsig, destSig) match {
        case (0, left) if left == size => assigned2
        case (0, _) => assigned2 :+ (lv |< destSig)
        case (_, left) if left == size => (lv |> destInsig) :+ assigned2 
        case (_, _) => (lv |> destInsig) :+ assigned2 :+ (lv |< destSig) 
      }
    }
    b2.buildAssign(inst, lv, assigned3)
  }
  
  private def toIR(inst: MoveInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    if (inst.doesKeep)
      builder.buildAssign(inst, lv, (inst.src |> 16) :+ (lv |< 16))
    else
      builder.buildAssign(inst, lv, inst.src)
  }
  
  private def toIR(inst: CompareInst, builder: IRBuilder) = {
    val cmp = inst.code match {
      case Test => inst.left & inst.right
      case Compare => inst.left - inst.right
      case CompareNeg => inst.left + inst.right
    }
    updateFlags(inst, cmp, builder)
  }
  
  private def toIR(inst: BranchInst, builder: IRBuilder) = {
    if (inst.isReturn)
      builder.buildRet(inst, inst.target)
    else if (inst.isCall)
      builder.buildCall(inst, inst.target)
    else
      builder.buildJmp(inst, inst.target)
  }
  
  private def toIR(ctx: Context, inst: LoadStoreInst, builder: IRBuilder) = {
    import AddressingMode._
    val addr: Expr = inst.addressingMode match {
      case PreIndex | PostIndex => inst.indexingOperand.base.get
      case Regular => inst.indexingOperand
    }
    val nbuilder = inst.addressingMode match {
      case PostIndex | Regular => builder
      case PreIndex =>
        builder.buildAssign(inst, Reg(inst.indexingOperand.base.get),
            inst.indexingOperand)
    }
    val b = inst match {
      case l: LoadInst => processLoadStore(l, addr, nbuilder)
      case l: LoadPairInst => processLoadStore(l, addr, nbuilder)
      case l: StoreInst => processLoadStore(l, addr, nbuilder)
      case l: StorePairInst => processLoadStore(l, addr, nbuilder)
    }
    inst.addressingMode match {
      case PreIndex | Regular => b
      case PostIndex =>
        b.buildAssign(inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand)
    }
  }
  
  private def processLoadStore(inst: LoadInst, addr: Expr, builder: IRBuilder) = {
    builder.buildLd(inst, Reg(inst.dest), addr)
  }
  
  private def processLoadStore(inst: LoadPairInst, addr: Expr, builder: IRBuilder) = {
    val first = Reg(inst.destLeft)
    val second = Reg(inst.destRight)
    val sizeInBytes = first.sizeInBits / 8
    builder.buildLd(inst, first, addr).buildLd(inst, second, addr + Const(sizeInBytes, wordSizeInBits))
  }
  
  private def processLoadStore(inst: StoreInst, addr: Expr, builder: IRBuilder) = {
    builder.buildSt(inst, addr, inst.src)
  }
  
  private def processLoadStore(inst: StorePairInst, addr: Expr, builder: IRBuilder) = {
    val first = Reg(inst.srcLeft)
    val second = Reg(inst.srcRight)
    val sizeInBytes = first.sizeInBits / 8
    builder.buildSt(inst, first, addr).buildSt(inst, second, addr + Const(sizeInBytes, wordSizeInBits))
  }
  
  private def toIR(inst: UnaryArithInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    import edu.psu.ist.plato.kaiming.aarch64.Opcode.Mnemonic._
    inst.opcode.mnemonic match {
      case ADR => builder.buildAssign(inst, lv, inst.src)
      case NEG => builder.buildAssign(inst, lv, Const(0, inst.src.sizeInBits) - inst.src)
      case _ => throw new UnreachableCodeException()
    }
  }
  
  private def toIR(inst: SelectInst, builder: IRBuilder) = {
    builder.buildSel(inst, Reg(inst.dest),
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
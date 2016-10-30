package edu.psu.ist.plato.kaiming.aarch64

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.AArch64
import edu.psu.ist.plato.kaiming.Arch.KaiMing

import edu.psu.ist.plato.kaiming.ir._

import edu.psu.ist.plato.kaiming.utils.Exception

object AArch64Machine extends Machine[AArch64] {
  
  override val returnRegister = Register.get(Register.Id.X0)
  override val wordSizeInBits = 64
  override val registers = 
    Register.Id.values.map(rid => Register.get(rid))
    .toSet[MachRegister[AArch64]]
  
  import scala.language.implicitConversions
  
  @inline
  private implicit def toExpr(i: Int): Const = Const(i, wordSizeInBits)
  
  @inline
  private implicit def toExpr(i: Long): Const = Const(i, wordSizeInBits)
  
  @inline
  private implicit def toExpr(f: Flag): Flg = Flg(f)
  
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
  
  @inline
  private implicit def toExpr(reg: Register): Expr = Reg(reg)
  
  @inline
  private implicit def toExpr(imm: Immediate): Expr = Const(imm.value, imm.sizeInBits)
  
  private implicit def toExpr(mem: Memory): Expr = {
    val ret: Option[Expr] = mem.base.map { x => x }
    mem.off match {
      case None => ret.get
      case Some(off) => {
        val oe: Expr = off match {
          case Left(imm) => Const(imm.value, wordSizeInBits)
          case Right(reg) => reg
        }
        ret match {
          case Some(expr) => expr + oe
          case None => oe
        }
      }
    }
  }
  
  private implicit def operandToExpr(op : Operand) = {
    val ret: Expr = op match {
      case imm: Immediate => imm
      case reg: Register => reg
      case mem: Memory => mem
      case sreg: ShiftedRegister => sreg
    }
    ret
  }
  
  private implicit def toExpr(cond: Condition): Expr = {
    import Flag._
    cond match {
      case Condition.AL => Const(1, 1)
      case Condition.NV => Const(0, 1)
      case Condition.EQ => Z
      case Condition.GE => N - ~V
      case Condition.GT => ~Z & ~(N ^ V)
      case Condition.HI => C & ~Z
      case Condition.HS => C
      case Condition.LE => Z | (N - Z)
      case Condition.LO => ~C
      case Condition.LS => Z | ~C
      case Condition.LT => N - V
      case Condition.MI => N
      case Condition.NE => ~Z
      case Condition.PL => ~N
      case Condition.VC => ~V
      case Condition.VS => V
    }
  }
    
  private def updateFlags(inst: Instruction, be: BExpr,
      builder: IRBuilder) = {
    import Flag._
    builder.buildAssign(inst, C, be @!).buildAssign(inst, N, be @-)
    .buildAssign(inst, Z, be @*).buildAssign(inst, V, be @^)
  }
  
  private def toIR(inst: BinaryArithInst, builder: IRBuilder) = {
    val lval = Reg(inst.dest.asRegister)
    import Opcode.Mnemonic.BinArith.Subtype._

    val rval = inst.subtype match {
      case ADD => inst.srcLeft + inst.srcRight
      case ADC => inst.srcLeft + inst.srcRight + Flag.C
      case SUB => inst.srcLeft - inst.srcRight
      case SBC => inst.srcLeft - inst.srcRight - Flag.C
      case MUL => inst.srcLeft * inst.srcRight
      case UMULL => (inst.srcLeft uext 64) * (inst.srcRight uext 64)
      case SMULL => (inst.srcLeft sext 64) * (inst.srcRight sext 64)
      case UMULH => ((inst.srcLeft uext 128) * (inst.srcRight uext 128)) |< 64
      case SMULH => ((inst.srcLeft sext 128) * (inst.srcRight sext 128)) |< 64
      case MNEG => -(inst.srcLeft * inst.srcRight)
      case SMNEGL => -((inst.srcLeft sext 64) * (inst.srcRight sext 64))
      case UMNEGL => -((inst.srcLeft uext 64) * (inst.srcRight uext 64))
      case SDIV => inst.srcLeft -/ inst.srcRight
      case UDIV => inst.srcLeft +/ inst.srcRight
      case ASR => inst.srcLeft >>> inst.srcRight
      case LSL => inst.srcLeft << inst.srcRight
      case LSR => inst.srcLeft >> inst.srcRight
      case ORR => inst.srcLeft | inst.srcRight
      case ORN => inst.srcLeft | ~inst.srcRight
      case AND => inst.srcLeft & inst.srcRight
      case EOR => inst.srcLeft ^ inst.srcRight
      case BIC => inst.srcLeft & ~inst.srcRight
      case EON => inst.srcLeft ^ ~inst.srcRight 
    }
    val nbuilder = builder.buildAssign(inst, lval, rval)
    if (inst.updateFlags)
      updateFlags(inst, rval, nbuilder)
    else
      nbuilder
  }
  
  private def toIR(inst: ExtendInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    val rv: Expr = if (inst.width == inst.src.sizeInBits) inst.src else inst.src |> inst.width 
    val e = inst.extension match {
      case Extension.Signed => rv sext lv.sizeInBits
      case Extension.Unsigned => rv uext lv.sizeInBits
    }
    builder.buildAssign(inst, lv, e)
  }
  
  private def toIR(ctx: Context, inst: BitfieldMoveInst, builder: IRBuilder) = {
    import Opcode.Mnemonic.BFMove.Subtype._
    
    val lv = Reg(inst.dest)
    val imm1 = inst.imm1.value.toInt
    val imm2 = inst.imm2.value.toInt
    val (rotate, shift) = inst.subtype match {
      case BFM | UBFM | SBFM => (imm1, imm2)
      case BFI | SBFIZ | UBFIZ => (-imm1 % lv.sizeInBits, imm2 - 1)
      case BFXIL | SBFX | UBFX => (imm1, imm1 + imm2 - 1)
    }
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
      case Some(Extension.Signed) => assigned2 sext size
      case Some(Extension.Unsigned) => assigned2 uext size
      case None => (destInsig, destSig) match {
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
    import Opcode.Mnemonic.Move.Subtype._
    val rv: Expr = inst.subtype match {
      case MOVK=>
        val op = inst.src.asImmediate
        val move = op.sizeInBits
        val retain = lv.sizeInBits - move
        if (retain > 0)
          (inst.src |> move) :+ (lv |< retain)
        else
          Const(op.value, lv.sizeInBits)
      case MOVN =>
        val op = inst.src.asImmediate
        val mask = (1 << lv.sizeInBits) - 1
        Const(~op.value & mask, lv.sizeInBits)
      case MOVZ | MOV =>
        inst.src
    }
    builder.buildAssign(inst, lv, rv)
  }
  
  private def toIR(inst: CompareInst, builder: IRBuilder) = {
    import Opcode.Mnemonic.Compare.Subtype._
    val cmp = inst.subtype match {
      case TST => inst.left & inst.right
      case CMP => inst.left - inst.right
      case CMN => inst.left + inst.right
    }
    updateFlags(inst, cmp, builder)
  }
  
  private def toIR(inst: BranchInst, builder: IRBuilder) = {
    if (inst.isReturn)
      builder.buildRet(inst, inst.target)
    else if (inst.isCall)
      builder.buildCall(inst, inst.target)
    else
      builder.buildJmp(inst, inst.condition, inst.target)
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
      case l: LoadInst => processLoad(l, addr, nbuilder)
      case l: LoadPairInst => processLoadPair(l, addr, nbuilder)
      case l: StoreInst => processStore(l, addr, nbuilder)
      case l: StorePairInst => processStorePair(l, addr, nbuilder)
      case l: StoreExclusiveInst => processStoreEx(l, addr, nbuilder)
      case l: StorePairExclusiveInst => processStorePairEx(l, addr, nbuilder)
    }
    inst.addressingMode match {
      case PreIndex | Regular => b
      case PostIndex =>
        b.buildAssign(inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand)
    }
  }
  
  private def processLoad(inst: LoadInst, addr: Expr, builder: IRBuilder) = {
    builder.buildLd(inst, Reg(inst.dest), addr)
  }
  
  private def processLoadPair(inst: LoadPairInst, addr: Expr, builder: IRBuilder) = {
    val first = Reg(inst.destLeft)
    val second = Reg(inst.destRight)
    val sizeInBytes = first.sizeInBits / 8
    builder.buildLd(inst, first, addr).buildLd(inst, second, addr + Const(sizeInBytes, wordSizeInBits))
  }
  
  private def processStore(inst: StoreInst, addr: Expr, builder: IRBuilder) = {
    builder.buildSt(inst, addr, inst.src)
  }
  
  private def processStorePair(inst: StorePairInst, addr: Expr, builder: IRBuilder) = {
    val first = Reg(inst.srcLeft)
    val second = Reg(inst.srcRight)
    val sizeInBytes = first.sizeInBits / 8
    builder.buildSt(inst, first, addr).buildSt(inst, second, addr + Const(sizeInBytes, wordSizeInBits))
  }
  
  private def processStoreEx(inst: StoreExclusiveInst, addr: Expr, builder: IRBuilder) = {
    builder.buildLd(inst, Reg(inst.src), addr)
    .buildAssign(inst, Reg(inst.result), Const(0, 32))
  }
  
  private def processStorePairEx(inst: StorePairExclusiveInst, addr: Expr, builder: IRBuilder) = {
    val first = Reg(inst.srcLeft)
    val second = Reg(inst.srcRight)
    val sizeInBytes = first.sizeInBits / 8
    builder.buildSt(inst, first, addr)
    .buildSt(inst, second, addr + Const(sizeInBytes, wordSizeInBits))
    .buildAssign(inst, Reg(inst.result), Const(0, 32))
  }
  
  private def toIR(inst: UnaryArithInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    import Opcode.Mnemonic.UnArith.Subtype._
    val rv = inst.subtype match {
      case NEG => Const(0, inst.src.sizeInBits) - inst.src
      case NGC => Const(0, inst.src.sizeInBits) - inst.src - 1 + Flag.C
      case MVN => ~inst.src
    }
    builder.buildAssign(inst, lv, rv)
  }
  
  private def toIR(inst: SelectInst, builder: IRBuilder) = {
    import Opcode.Mnemonic._
    val size = inst.dest.sizeInBits
    val (tv: Expr, fv: Expr) = inst match {
      case i: UnarySelectInst =>
        import UnSel.Subtype._ 
        i.subtype match {
          case CSET => (Const(1, size), Const(0, size))
          case CSETM => (Const(1 << size - 1, size), Const(0, size))
        }
      case i: BinarySelectInst =>
        import BinSel.Subtype._
        val src = i.src
        i.subtype match {
          case CINC => (src + Const(1, size), src)
          case CNEG => (-src, src)
          case CINV => (~src, src)
        }
      case i: TrinarySelectInst =>
        import TriSel.Subtype._
        val st = i.srcTrue
        val sf = i.srcFalse
        i.subtype match {
          case CSEL => (st, sf)
          case CSINV => (st, ~sf)
          case CSNEG => (st, -sf)
          case CSINC => (st, sf + 1)
        }
        (i.srcTrue, i.srcFalse)
    }
    builder.buildSel(inst, Reg(inst.dest), inst.condition, tv, fv)
    
  }
  
  private def toIR(inst: CompBranchInst, builder: IRBuilder) = {
    import Opcode.Mnemonic.CompBranch.Subtype._
    val cond: Expr = inst.subtype match {
      case CBNZ => !inst.toCompare
      case CBZ => inst.toCompare
    }
    builder.buildJmp(inst, cond, inst.target)
  }
  
  private def toIR(inst: TestBranchInst, builder: IRBuilder) = {
    import Opcode.Mnemonic.TestBranch.Subtype._
    val cond: Expr = inst.subtype match {
      case TBNZ => !(inst.toTest & inst.imm)
      case TBZ => inst.toTest & inst.imm
    }
    builder.buildJmp(inst, cond, inst.target)
  }
  
  private def toIR(inst: DataProcessInst, builder: IRBuilder) = {
    import Opcode.Mnemonic.DataProcess.Subtype._
    inst.subtype match {
      case REV => builder.buildAssign(inst, Reg(inst.dest), inst.src bswap)
      case CLS => Exception.unsupported()
      case CLZ => Exception.unsupported()
      case RBIT => Exception.unsupported()
      case REV16 => Exception.unsupported()
      case REV32 => Exception.unsupported()
    }
  }
  
  private def toIR(inst: TrinaryArithInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    import Opcode.Mnemonic.TriArith.Subtype._
    
    val rv = inst.subtype match {
      case MADD => inst.src3 + inst.src1 * inst.src2
      case MSUB => inst.src3 - inst.src1 * inst.src2
      case SMADDL => inst.src3 + (inst.src1 sext 64) * (inst.src2 sext 64)
      case SMSUBL => inst.src3 - (inst.src1 sext 64) * (inst.src2 sext 64)
      case UMADDL => inst.src3 + (inst.src1 uext 64) * (inst.src2 uext 64)
      case UMSUBL => inst.src3 + (inst.src1 uext 64) * (inst.src2 uext 64)
    }
    
    builder.buildAssign(inst, lv, rv)
    
  }
  
  private def toIR(inst: CondCompareInst, builder: IRBuilder) = {
    Exception.unsupported()
  }
  
  private def toIR(inst: PCRelativeInst, builder: IRBuilder) = {
    builder.buildAssign(inst, Reg(inst.dest), inst.ptr)
  }
  
  override protected def toIRStatements(ctx: Context, inst: MachEntry[AArch64],
      builder: IRBuilder) = {
    inst.asInstanceOf[Instruction] match {
      case i: UnaryArithInst => toIR(i, builder)
      case i: TrinaryArithInst => toIR(i, builder)
      case i: BinaryArithInst => toIR(i, builder)
      case i: BitfieldMoveInst => toIR(ctx, i, builder)
      case i: ExtendInst => toIR(i, builder)
      case i: BranchInst => toIR(i, builder)
      case i: CompBranchInst => toIR(i, builder)
      case i: TestBranchInst => toIR(i, builder)
      case i: CompareInst => toIR(i, builder)
      case i: CondCompareInst => toIR(i, builder)
      case i: MoveInst => toIR(i, builder)
      case i: SelectInst => toIR(i, builder)
      case i: LoadStoreInst => toIR(ctx, i, builder)
      case i: DataProcessInst => toIR(i, builder)
      case i: PCRelativeInst => toIR(i, builder)
      case i: NopInst => builder
      case i: SystemInst => builder
    }
  }

}
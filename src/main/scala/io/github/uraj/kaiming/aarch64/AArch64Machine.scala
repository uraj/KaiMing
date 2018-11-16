package io.github.uraj.kaiming.aarch64

import io.github.uraj.kaiming._
import io.github.uraj.kaiming.ir._
import io.github.uraj.kaiming.utils.Exception

object AArch64Machine extends Machine[AArch64] {
  
  override val returnRegister = Register.get(Register.Id.X0)
  @inline override val wordSizeInBits = 64
  override val registers = 
    Register.Id.values.map(rid => Register.get(rid))
    .toSet[MachRegister[AArch64]]
  
  import scala.language.implicitConversions
  
  @inline private implicit def toExpr(i: Int): Const = Const(i, wordSizeInBits)
  
  @inline private implicit def toExpr(i: Long): Const = Const(i, wordSizeInBits)
  
  @inline private implicit def toExpr(f: Flag): Flg = Flg(f)
  
  private def mregToExpr(oracle: Int)(mreg: ModifiedRegister): Expr = {
    val reg = Reg(mreg.reg)
    mreg.modifier match {
      case Asr(v) => reg >>> v
      case Lsl(v) => reg << v
      case Ror(v) => reg >< v
      case Lsr(v) => reg >> v
      case ext: RegExtension =>
        val truncated = if (ext.truncate >= reg.sizeInBits) {
          reg
        } else {
          reg |> ext.truncate
        }
        val extended = if (ext.isSigned) {
          truncated sext oracle
        } else {
          truncated uext oracle
        }
        if (ext.lsl == 0) extended
        else extended << ext.lsl
    }
  }
  
  @inline
  private implicit def toExpr(reg: Register): Expr = Reg(reg)
  
  private implicit def toExpr(mem: Memory): Expr = {
    val ret: Option[Expr] = mem.base.map { x => x }
    mem.off match {
      case None => ret.get
      case Some(off) => {
        val oe: Expr = off match {
          case Left(imm) => Const(imm.value, wordSizeInBits)
          case Right(reg) => mregToExpr(wordSizeInBits)(reg)
        }
        ret match {
          case Some(expr) => expr + oe
          case None => oe
        }
      }
    }
  }

  private def operandToExpr(oracle: Int, op : Operand) = {
    val ret: Expr = op match {
      case imm: Immediate => Const(imm.value, oracle)
      case reg: Register => reg
      case mem: Memory => mem
      case sreg: ModifiedRegister => mregToExpr(oracle)(sreg)
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
      builder: IRBuilder[AArch64]) = {
    import Flag._
    builder.assign(inst, C, be @!).assign(inst, N, be @-)
    .assign(inst, Z, be @*).assign(inst, V, be @^)
  }
  
  private def toIR(inst: BinaryArithInst, builder: IRBuilder[AArch64]) = {
    val lval = Reg(inst.dest.asRegister)
    import Opcode.OpClass.BinArith.Mnemonic._
    
    val right = operandToExpr(inst.srcLeft.sizeInBits, inst.srcRight)
    
    val rval = inst.subtype match {
      case ADD | ADDS => inst.srcLeft + right
      case ADC | ADCS => inst.srcLeft + right + (Flag.C uext right.sizeInBits)
      case SUB | SUBS => inst.srcLeft - right
      case SBC | SBCS => inst.srcLeft - right - (Flag.C uext right.sizeInBits)
      case MUL => inst.srcLeft * right
      case UMULL => (inst.srcLeft uext 64) * (right uext 64)
      case SMULL => (inst.srcLeft sext 64) * (right sext 64)
      case UMULH => ((inst.srcLeft uext 128) * (right uext 128)) |< 64
      case SMULH => ((inst.srcLeft sext 128) * (right sext 128)) |< 64
      case MNEG => -(inst.srcLeft * right)
      case SMNEGL => -((inst.srcLeft sext 64) * (right sext 64))
      case UMNEGL => -((inst.srcLeft uext 64) * (right uext 64))
      case SDIV | SDIVS => inst.srcLeft -/ right
      case UDIV | UDIVS => inst.srcLeft +/ right
      case ASR => inst.srcLeft >>> right
      case LSL => inst.srcLeft << right
      case LSR => inst.srcLeft >> right
      case ORR => inst.srcLeft | right
      case ORN => inst.srcLeft | ~right
      case AND | ANDS => inst.srcLeft & right
      case EOR => inst.srcLeft ^ right
      case BIC | BICS => inst.srcLeft & ~right
      case EON => inst.srcLeft ^ ~right 
    }
    val nbuilder = builder.assign(inst, lval, rval)
    if (inst.updateFlags)
      updateFlags(inst, rval, nbuilder)
    else
      nbuilder
  }
  
  private def toIR(inst: ExtendInst, builder: IRBuilder[AArch64]) = {
    val lv = Reg(inst.dest)
    val rv: Expr = if (inst.width == inst.src.sizeInBits) inst.src else inst.src |> inst.width 
    val e = inst.extension match {
      case Extension.Signed => rv sext lv.sizeInBits
      case Extension.Unsigned => rv uext lv.sizeInBits
    }
    builder.assign(inst, lv, e)
  }
  
  private def toIR(inst: BitfieldMoveInst, builder: IRBuilder[AArch64]) = {
    import Opcode.OpClass.BFMove.Mnemonic._
    
    val lv = Reg(inst.dest)
    val imm1 = inst.imm1.value.toInt
    val imm2 = inst.imm2.value.toInt
    val size = lv.sizeInBits
    val (rotate, shift) = inst.subtype match {
      case BFM | UBFM | SBFM => (imm1, imm2)
      case BFI | SBFIZ | UBFIZ => (((-imm1 % size) + size) % size, imm2 - 1)
      case BFXIL | SBFX | UBFX => (imm1, imm1 + imm2 - 1)
    }
    val (destInsig, destSig, srcInsig, srcSig) =
      if (shift >= rotate) (0, shift - rotate + 1, rotate, shift + 1)
      else (size - rotate, size + shift - rotate + 1, 0, shift + 1)
    val assigned: Expr =
      if (srcInsig > 0) (inst.src |> srcSig) |< (srcInsig) else inst.src |> srcSig
    val assigned2 = inst.extension match {
      case Some(Extension.Signed) => 
        (if (srcInsig > 0) assigned :+ Const(0, srcInsig) else assigned) sext size
      case Some(Extension.Unsigned) =>
        (if (srcInsig > 0) assigned :+ Const(0, srcInsig) else assigned) uext size
      case None => (destInsig, destSig) match {
        case (0, left) if left == size => assigned
        case (0, _) => assigned :+ (lv |< destSig)
        case (_, left) if left == size => (lv |> destInsig) :+ assigned 
        case (_, _) => (lv |> destInsig) :+ assigned :+ (lv |< destSig) 
      }
    }
    builder.assign(inst, lv, assigned2)
  }
  
  private def toIR(inst: MoveInst, builder: IRBuilder[AArch64]) = {
    val lv = Reg(inst.dest)
    import Opcode.OpClass.Move.Mnemonic._
    val rv: Expr = inst.subtype match {
      case MOVK =>
        val op = inst.src.asImmediate
        val retain = 16 + op.lShift
        if (retain < lv.sizeInBits && op.lShift > 0)
          (lv |< retain) :+ Const(op.value, 16) :+ (lv |> op.lShift) 
        else if (op.lShift > 0)
          Const(op.value, 16) :+ (lv |> op.lShift)
        else
          (lv |< retain) :+ Const(op.value, 16) 
      case MOVN =>
        val op = inst.src.asImmediate
        val mask = (1 << lv.sizeInBits) - 1
        Const(~op.value & mask, lv.sizeInBits)
      case MOVZ | MOV =>
        inst.src match {
          case r: Register => r
          case i: Immediate => Const(i.value, lv.sizeInBits)
          case _ => Exception.unreachable()
        }
    }
    builder.assign(inst, lv, rv)
  }
  
  private def toIR(inst: CompareInst, builder: IRBuilder[AArch64]) = {
    import Opcode.OpClass.Compare.Mnemonic._
    
    val right = operandToExpr(inst.left.sizeInBits, inst.right)
    
    val cmp = inst.subtype match {
      case TST => inst.left & right
      case CMP => inst.left - right
      case CMN => inst.left + right
    }
    updateFlags(inst, cmp, builder)
  }
  
  private def toIR(inst: BranchInst, builder: IRBuilder[AArch64]) = {
    val target = operandToExpr(wordSizeInBits, inst.target)
    if (inst.isReturn)
      builder.ret(inst, target)
    else if (inst.isCall)
      builder.call(inst, target)
    else
      builder.jump(inst, inst.condition, target)
  }
  
  private def toIR(inst: LoadStoreInst, builder: IRBuilder[AArch64]) = {
    import AddressingMode._
    val addr: Expr = inst.addressingMode match {
      case PreIndex | PostIndex => inst.indexingOperand.base.get
      case Regular => inst.indexingOperand
    }
    val nbuilder = inst.addressingMode match {
      case PostIndex | Regular => builder
      case PreIndex =>
        builder.assign(inst, Reg(inst.indexingOperand.base.get),
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
        b.assign(inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand)
    }
  }
  
  private def processLoad(inst: LoadInst, addr: Expr, builder: IRBuilder[AArch64]) = {
    builder.load(inst, Reg(inst.dest), addr)
  }
  
  private def processLoadPair(inst: LoadPairInst, addr: Expr, builder: IRBuilder[AArch64]) = {
    val first = Reg(inst.destLeft)
    val second = Reg(inst.destRight)
    val sizeInBytes = first.sizeInBits / 8
    builder.load(inst, first, addr).load(inst, second, addr + Const(sizeInBytes, wordSizeInBits))
  }
  
  private def processStore(inst: StoreInst, addr: Expr, builder: IRBuilder[AArch64]) = {
    builder.store(inst, addr, inst.src)
  }
  
  private def processStorePair(inst: StorePairInst, addr: Expr, builder: IRBuilder[AArch64]) = {
    val first = Reg(inst.srcLeft)
    val second = Reg(inst.srcRight)
    val sizeInBytes = first.sizeInBits / 8
    builder.store(inst, first, addr).store(inst, second, addr + Const(sizeInBytes, wordSizeInBits))
  }
  
  private def processStoreEx(inst: StoreExclusiveInst, addr: Expr, builder: IRBuilder[AArch64]) = {
    builder.load(inst, Reg(inst.src), addr)
    .assign(inst, Reg(inst.result), Const(0, 32))
  }
  
  private def processStorePairEx(inst: StorePairExclusiveInst, addr: Expr, builder: IRBuilder[AArch64]) = {
    val first = Reg(inst.srcLeft)
    val second = Reg(inst.srcRight)
    val sizeInBytes = first.sizeInBits / 8
    builder.store(inst, first, addr)
    .store(inst, second, addr + Const(sizeInBytes, wordSizeInBits))
    .assign(inst, Reg(inst.result), Const(0, 32))
  }
  
  private def toIR(inst: UnaryArithInst, builder: IRBuilder[AArch64]) = {
    val lv = Reg(inst.dest)
    val src = operandToExpr(lv.sizeInBits, inst.src)
    val size = inst.dest.sizeInBits
    import Opcode.OpClass.UnArith.Mnemonic._
    val rv = inst.subtype match {
      case NEG | NEGS => Const(0, size) - src
      case NGC | NGCS => Const(0, size) - src - Const(1, size) + (Flag.C uext size)
      case MVN => ~src
    }
    val nbuilder = builder.assign(inst, lv, rv)
    if (inst.updateFlags)
      updateFlags(inst, rv.asInstanceOf[BExpr], nbuilder)
    else
      nbuilder
  }
  
  private def toIR(inst: SelectInst, builder: IRBuilder[AArch64]) = {
    import Opcode.OpClass._
    val size = inst.dest.sizeInBits
    val (tv, fv): (Expr, Expr) = inst match {
      case i: UnarySelectInst =>
        import UnSel.Mnemonic._ 
        i.subtype match {
          case CSET => (Const(1, size), Const(0, size))
          case CSETM => (Const(1 << size - 1, size), Const(0, size))
        }
      case i: BinarySelectInst =>
        import BinSel.Mnemonic._
        val src = i.src
        i.subtype match {
          case CINC => (src + Const(1, size), src)
          case CNEG => (-src, src)
          case CINV => (~src, src)
        }
      case i: TrinarySelectInst =>
        import TriSel.Mnemonic._
        val st = i.srcTrue
        val sf = i.srcFalse
        i.subtype match {
          case CSEL => (st, sf)
          case CSINV => (st, ~sf)
          case CSNEG => (st, -sf)
          case CSINC => (st, sf + Const(1, size))
        }
        (i.srcTrue, i.srcFalse)
    }
    builder.select(inst, Reg(inst.dest), inst.condition, tv, fv)
    
  }
  
  private def toIR(inst: CompBranchInst, builder: IRBuilder[AArch64]) = {
    import Opcode.OpClass.CompBranch.Mnemonic._
    val cond: Expr = inst.subtype match {
      case CBNZ => !inst.toCompare
      case CBZ => inst.toCompare
    }
    builder.jump(inst, cond, inst.target)
  }
  
  private def toIR(inst: TestBranchInst, builder: IRBuilder[AArch64]) = {
    import Opcode.OpClass.TestBranch.Mnemonic._
    val c = Const(inst.imm.value, inst.toTest.sizeInBits)
    val cond: Expr = inst.subtype match {
      case TBNZ => !(inst.toTest & c)
      case TBZ => inst.toTest & c
    }
    builder.jump(inst, cond, inst.target)
  }
  
  private def toIR(inst: DataProcessInst, builder: IRBuilder[AArch64]) = {
    import Opcode.OpClass.DataProcess.Mnemonic._
    val lv = Reg(inst.dest)
    val rv = inst.subtype match {
      case CLS => ((~inst.src) clz) uext inst.dest.sizeInBits
      case CLZ => (inst.src clz) uext inst.dest.sizeInBits
      case RBIT => inst.src bswap 1
      case REV => inst.src bswap 8
      case REV16 => inst.src bswap 16
      case REV32 => inst.src bswap 32
    }
    builder.assign(inst, lv, rv)
  }
  
  private def toIR(inst: TrinaryArithInst, builder: IRBuilder[AArch64]) = {
    val lv = Reg(inst.dest)
    import Opcode.OpClass.TriArith.Mnemonic._
    
    val rv = inst.subtype match {
      case MADD => inst.src3 + inst.src1 * inst.src2
      case MSUB => inst.src3 - inst.src1 * inst.src2
      case SMADDL => inst.src3 + (inst.src1 sext 64) * (inst.src2 sext 64)
      case SMSUBL => inst.src3 - (inst.src1 sext 64) * (inst.src2 sext 64)
      case UMADDL => inst.src3 + (inst.src1 uext 64) * (inst.src2 uext 64)
      case UMSUBL => inst.src3 + (inst.src1 uext 64) * (inst.src2 uext 64)
    }
    
    builder.assign(inst, lv, rv)
    
  }
  
  private def toIR(inst: CondCompareInst, builder: IRBuilder[AArch64]) = {
    import Opcode.OpClass.CondCompare.Mnemonic._
    val right = operandToExpr(inst.left.sizeInBits, inst.right)
    val cmp = inst.subtype match {
      case CCMP => inst.left - right
      case CCMN => inst.left + right
    }
    val cond: Expr = inst.condition
    builder
    .select(inst, Flag.V, cond, cmp @^, Const(inst.nzcv.value & (1 << 0), 1))
    .select(inst, Flag.C, cond, cmp @!, Const(inst.nzcv.value & (1 << 1), 1))
    .select(inst, Flag.Z, cond, cmp @*, Const(inst.nzcv.value & (1 << 2), 1))
    .select(inst, Flag.N, cond, cmp @-, Const(inst.nzcv.value & (1 << 3), 1))
  }
  
  private def toIR(inst: PCRelativeInst, builder: IRBuilder[AArch64]) = {
    builder.assign(inst, Reg(inst.dest), Const(inst.ptr.value, wordSizeInBits))
  }
  
  override def toIRStatements(inst: Entry[AArch64],
      builder: IRBuilder[AArch64]): IRBuilder[AArch64] = {
    inst.asInstanceOf[Instruction] match {
      case i: UnaryArithInst => toIR(i, builder)
      case i: TrinaryArithInst => toIR(i, builder)
      case i: BinaryArithInst => toIR(i, builder)
      case i: BitfieldMoveInst => toIR(i, builder)
      case i: ExtendInst => toIR(i, builder)
      case i: BranchInst => toIR(i, builder)
      case i: CompBranchInst => toIR(i, builder)
      case i: TestBranchInst => toIR(i, builder)
      case i: CompareInst => toIR(i, builder)
      case i: CondCompareInst => toIR(i, builder)
      case i: MoveInst => toIR(i, builder)
      case i: SelectInst => toIR(i, builder)
      case i: LoadStoreInst => toIR(i, builder)
      case i: DataProcessInst => toIR(i, builder)
      case i: PCRelativeInst => toIR(i, builder)
      case i: NopInst => builder.nop(i)
      case i: SystemInst => builder.unsupported(i)
      case i: UnsupportedInst => builder.unsupported(i)
    }
  }

}

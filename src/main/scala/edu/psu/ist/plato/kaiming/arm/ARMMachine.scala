package edu.psu.ist.plato.kaiming.arm

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.ARM

import edu.psu.ist.plato.kaiming.ir._

import edu.psu.ist.plato.kaiming.exception.UnreachableCodeException

object ARMMachine extends Machine[ARM] {
  
  override val returnRegister: Register = Register.Id.R0
  override val wordSizeInBits = 32
  override val registers = 
    Register.Id.values.map(rid => rid: Register)
    .toSet[MachRegister[ARM]]
  
    import scala.language.implicitConversions
  
  private implicit def toExpr(imm: Immediate): Const = Const(imm.value)
  
  private implicit def toExpr(reg: Register): Expr = {
    val ret = Reg(reg)
    reg.shift match {
      case None => ret
      case Some(shift) => shift match {
        case Asr(v) => ret >>> Const(v)
        case Lsl(v) => ret << Const(v)
        case Ror(v) => ret >< Const(v)
        case Lsr(v) => ret >> Const(v)
        case Rrx() => ret >< Const(1)
      }
    }
  }
  
  private implicit def toExpr(mem: Memory): Expr = {
    val left = mem.base
    mem.off match {
      case Left(imm) => left match {
        case None => Const(imm)
        case Some(expr) =>
          if (imm == 0) expr 
          else if (imm > 0) expr + Const(imm)
          else expr - Const(-imm)
      }
      case Right((sign, reg)) => sign match {
        case Positive => left.get + reg
        case Negative => left.get - reg
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
    case Condition.GE => !Flg(Flag.N) - Flg(Flag.V)
    case Condition.GT => !Flg(Flag.Z) & !(Flg(Flag.N) ^ Flg(Flag.V))
    case Condition.HI => Flg(Flag.C) & !Flg(Flag.Z)
    case Condition.HS => Flg(Flag.C)
    case Condition.LE => Flg(Flag.Z) | (Flg(Flag.N) ^ Flg(Flag.Z))
    case Condition.LO => !Flg(Flag.C)
    case Condition.LS => Flg(Flag.Z) | !Flg(Flag.C)
    case Condition.LT => Flg(Flag.N) ^ Flg(Flag.V)
    case Condition.MI => Flg(Flag.N)
    case Condition.NE => !Flg(Flag.Z)
    case Condition.PL => !Flg(Flag.N)
    case Condition.VC => !Flg(Flag.V)
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
    import edu.psu.ist.plato.kaiming.arm.Opcode.Mnemonic._
    val rval = inst.opcode.mnemonic match {
      case ADD => {
        val add = inst.srcLeft + inst.srcRight
        if (inst.opcode.rawcode.charAt(2) == 'C')
          add + (Flg(Flag.C))
        else
          add
      }
      case SUB => {
        val sub =
          if (inst.opcode.rawcode.startsWith("RSB"))
             inst.srcLeft - inst.srcRight
          else inst.srcRight - (inst.srcLeft)
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
      case BIC => inst.srcLeft & !inst.srcRight
      case EOR => inst.srcLeft ^ inst.srcRight
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
  
  private def toIR(ctx: Context, inst: ExtractInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    val shift = inst.lsb.value.toInt
    val mask = 1 << inst.width.value.toInt - 1
    val size = lv.sizeInBits
    val assigned = (inst.src << Const(shift)) + Const(mask)
    val assigned2 = inst.extension match {
      case Extension.Signed => assigned.sext(Const(size))
      case Extension.Unsigned => assigned.uext(Const(size))
    }
    builder + AssignStmt(builder.nextIndex, inst, lv, assigned2)
  }
  
  private def toIR(inst: MoveInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    if (inst.isConditional) {
      builder + SelStmt(builder.nextIndex, inst, lv, inst.condition, inst.src, lv)
    }
    else {
      if (inst.isMoveTop)
        builder + AssignStmt(builder.nextIndex, inst, lv, (inst.src uext Const(16)) :: (lv |> Const(16)))
      else
        builder + AssignStmt(builder.nextIndex, inst, lv, inst.src)
    }
  }
  
  private def toIR(inst: CompareInst, builder: IRBuilder) = {
    val cmp = inst.code match {
      case CompareCode.Test => inst.left ^ inst.right
      case CompareCode.Compare => inst.left - inst.right
      case CompareCode.CompareNeg => inst.left + inst.right
      case CompareCode.TestEq => inst.left ^ inst.right
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
    inst match {
      case l: LoadInst => processLoadStore(l, builder)
      case l: LoadMultipleInst => processLoadStore(l, builder)
      case l: StoreInst => processLoadStore(l, builder)
      case l: StoreMultipleInst => processLoadStore(l, builder)
    }
  }
  
  private def processLoadStore(inst: LoadInst, builder: IRBuilder) = {
    import AddressingMode._
    val (addr, b) = inst.addressingMode match {
      case PostIndex => (inst.indexingOperand.base.get: Expr, builder)
      case PreIndex => {
        val ea: Expr = inst.indexingOperand.base.get
        (ea, builder + AssignStmt(builder.nextIndex, inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand))  
      }
      case Regular => (inst.indexingOperand: Expr, builder)
    }
    val bb = b + LdStmt(b.nextIndex, inst, Reg(inst.dest), addr)
    inst.addressingMode match {
      case PostIndex =>
        bb + AssignStmt(bb.nextIndex, inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand)
      case PreIndex | Regular => bb
    }
  }
  
  private def processLoadStore(inst: LoadMultipleInst, builder: IRBuilder) = {
    // ARM usually uses POP PC as return 
    val sizeInBytes = wordSizeInBits / 8
    val base: Reg = Reg(inst.base.base.get)
    val load = inst.destList.foldLeft(builder) {
      case (b, reg) => {
        val stb = b + LdStmt(b.nextIndex, inst, Reg(reg), base)
        if (inst.preindex)
          stb + AssignStmt(stb.nextIndex, inst, base, base + Const(sizeInBytes))
        else
          stb
      }
    }
    if (inst.destList.contains(Register(Register.Id.PC, None)))
      load + RetStmt(load.nextIndex, inst, Reg(Register.Id.PC))
    else
      load
  }
  
  private def processLoadStore(inst: StoreInst, builder: IRBuilder) = {
    import AddressingMode._
    val (addr, b) = inst.addressingMode match {
      case PostIndex => (inst.indexingOperand.base.get: Expr, builder)
      case PreIndex => {
        val ea: Expr = inst.indexingOperand.base.get
        (ea, builder + AssignStmt(builder.nextIndex, inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand))
      }
      case Regular => (inst.indexingOperand: Expr, builder)
    }
    val bb = b + StStmt(b.nextIndex, inst, addr, inst.src)
    inst.addressingMode match {
      case PostIndex =>
        bb + AssignStmt(bb.nextIndex, inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand)
      case PreIndex | Regular => bb
    }
  }
  
  private def processLoadStore(inst: StoreMultipleInst, builder: IRBuilder) = {
    val sizeInBytes = wordSizeInBits / 8
    val base = Reg(inst.base.base.get)
    inst.srcList.foldLeft(builder) {
      case (b, reg) => {
        val stb = b + StStmt(b.nextIndex, inst, Reg(reg), base)
        if (inst.preindex)
          stb + AssignStmt(stb.nextIndex, inst, base, base + Const(sizeInBytes))
        else
          stb
      }
    }
  }
  
  private def toIR(inst: UnaryArithInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    import edu.psu.ist.plato.kaiming.arm.Opcode.Mnemonic._
    builder + {
      inst.opcode.mnemonic match {
        case NOT => AssignStmt(builder.nextIndex, inst, lv, Const(0) - inst.src)
        case ADR => AssignStmt(builder.nextIndex, inst, lv, inst.src)
        case _ => throw new UnreachableCodeException()
      }
    }
  }
  
  private def toIR(ctx: Context, inst: LongMulInst, builder: IRBuilder) = {
    val tmpVar = ctx.getNewTempVar(64)
    val b1 = builder + AssignStmt(builder.nextIndex, inst, tmpVar, inst.srcLeft * inst.srcRight)
    val b2 = b1 + AssignStmt(b1.nextIndex, inst, Reg(inst.destHi), tmpVar |< Const(31))
    b2 + AssignStmt(b2.nextIndex, inst, Reg(inst.destLow), tmpVar |> Const(31))
  }
  
  private def toIR(inst: BitfieldClearInst, builder: IRBuilder) = {
    val lv = Reg(inst.dest)
    val low = Const(inst.lsb.value)
    val high = Const(inst.lsb.value + inst.width.value)
    builder + AssignStmt(builder.nextIndex, inst, lv, (lv |<  high) :: ((lv |> low) uext high))
  }
  
  private def toIR(inst: BitfieldInsertInst, builder: IRBuilder) = {
    val low = Const(inst.lsb.value)
    val width = Const(inst.width.value)
    val high = Const(inst.width.value + inst.lsb.value)
    val lv = Reg(inst.dest)
    builder + AssignStmt(builder.nextIndex, inst, lv,
        (lv |< high) :: (inst.src |> width) :: (lv |> low))
  }
  
  override protected def toIRStatements(ctx: Context, inst: MachEntry[ARM],
      builder: IRBuilder) = {
    inst.asInstanceOf[Instruction] match {
      case i: BinaryArithInst => toIR(i, builder)
      case i: UnaryArithInst => toIR(i, builder)
      case i: LongMulInst => toIR(ctx, i, builder)
      case i: ExtractInst => toIR(ctx, i, builder)
      case i: ExtensionInst => toIR(i, builder)
      case i: BranchInst => toIR(i, builder)
      case i: CompareInst => toIR(i, builder)
      case i: MoveInst => toIR(i, builder)
      case i: LoadStoreInst => toIR(ctx, i, builder)
      case i: BitfieldClearInst => toIR(i, builder)
      case i: BitfieldInsertInst => toIR(i, builder)
      case i: NopInst => builder
    }
  }

  
}
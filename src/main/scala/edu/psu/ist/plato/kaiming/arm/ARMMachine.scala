package edu.psu.ist.plato.kaiming.arm

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.ir._
import edu.psu.ist.plato.kaiming.utils.Exception

object ARMMachine extends Machine[ARM] {
  
  override val returnRegister: Register = Register.Id.R0
  override val wordSizeInBits = 32
  override val registers = 
    Register.Id.values.map(rid => rid: Register)
    .toSet[MachRegister[ARM]]
  
    import scala.language.implicitConversions
  
  private implicit def toExpr(i: Int): Const = Const(i, wordSizeInBits)
  private implicit def toExpr(i: Long): Const = Const(i, wordSizeInBits)
  
  private implicit def toExpr(reg: Register): Expr = {
    val ret = Reg(reg)
    reg.shift match {
      case None => ret
      case Some(shift) => shift match {
        case Asr(v) => ret >>> v
        case Lsl(v) => ret << v
        case Ror(v) => ret >< v
        case Lsr(v) => ret >> v
        case Rrx() => ret >< 1
      }
    }
  }
  
  private implicit def toExpr(mem: Memory): Expr = {
    val left = mem.base
    mem.off match {
      case Left(imm) => left match {
        case None => imm
        case Some(expr) =>
          if (imm == 0) expr 
          else if (imm > 0) expr + imm
          else expr - (-imm)
      }
      case Right((sign, reg)) => sign match {
        case Positive => left.get + reg
        case Negative => left.get - reg
      }
    }
  }
  
  private implicit def operandToExpr(op : Operand): Expr = {
    op match {
      case imm: Immediate => imm.value
      case reg: Register => reg
      case mem: Memory => mem
    }
  }
  
  private implicit def toExpr(cond: Condition) = cond match {
    case Condition.AL | Condition.NV => Const(1, 1)
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
    
  private def updateFlags(inst: Instruction, be: BExpr,
      builder: IRBuilder[ARM]) = {
    builder.assign(inst, Flg(Flag.C), be @!)
    .assign(inst, Flg(Flag.N), be @-)
    .assign(inst, Flg(Flag.Z), be @*)
    .assign(inst, Flg(Flag.V), be @^)
  }
  
  private def toIR(inst: BinaryArithInst, builder: IRBuilder[ARM]) = {
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
      case SDIV => inst.srcLeft -/ inst.srcRight
      case UDIV => inst.srcLeft +/ inst.srcRight
      case ASR => inst.srcLeft >>> inst.srcRight
      case LSL => inst.srcLeft << inst.srcRight
      case LSR => inst.srcLeft >> inst.srcRight
      case ORR => inst.srcLeft | inst.srcRight
      case ORN => inst.srcLeft | !inst.srcRight
      case AND => inst.srcLeft & inst.srcRight
      case BIC => inst.srcLeft & !inst.srcRight
      case EOR => inst.srcLeft ^ inst.srcRight
      case _ => Exception.unreachable()
    }
    val nbuilder = builder.assign(inst, lval, rval)
    if (inst.updateFlags)
      updateFlags(inst, rval, nbuilder)
    else
      nbuilder
  }
  
  private def toIR(inst: ExtensionInst, builder: IRBuilder[ARM]) = {
    val lv = Reg(inst.dest)
    val e = inst.extension match {
      case Extension.Signed => lv sext lv.sizeInBits
      case _ => lv uext lv.sizeInBits
    }
    builder.assign(inst, lv, e)
  }
  
  private def toIR(inst: ExtractInst, builder: IRBuilder[ARM]) = {
    val lv = Reg(inst.dest)
    val shift = inst.lsb.value.toInt
    val mask = 1 << inst.width.value.toInt - 1
    val size = lv.sizeInBits
    val assigned = (inst.src << shift) + Const(mask, inst.src.sizeInBits)
    val assigned2 = inst.extension match {
      case Extension.Signed => assigned sext size
      case Extension.Unsigned => assigned uext size
    }
    builder.assign(inst, lv, assigned2)
  }
  
  private def toIR(inst: MoveInst, builder: IRBuilder[ARM]) = {
    val lv = Reg(inst.dest)
    if (inst.isConditional) {
      builder.select(inst, lv, inst.condition, inst.src, lv)
    }
    else {
      if (inst.isMoveTop)
        builder.assign(inst, lv, (lv |> 16) :+ (inst.src uext 16))
      else
        builder.assign(inst, lv, inst.src)
    }
  }
  
  private def toIR(inst: CompareInst, builder: IRBuilder[ARM]) = {
    val cmp = inst.code match {
      case CompareCode.Test => inst.left ^ inst.right
      case CompareCode.Compare => inst.left - inst.right
      case CompareCode.CompareNeg => inst.left + inst.right
      case CompareCode.TestEq => inst.left ^ inst.right
    }
    updateFlags(inst, cmp, builder)
  }
  
  private def toIR(inst: BranchInst, builder: IRBuilder[ARM]) = {
    if (inst.isReturn)
      builder.ret(inst, inst.target)
    else if (inst.isCall)
      builder.call(inst, inst.target)
    else
      builder.jump(inst, inst.condition, inst.target)
  }
  
  private def toIR(inst: LoadStoreInst, builder: IRBuilder[ARM]) = {
    inst match {
      case l: LoadInst => processLoadStore(l, builder)
      case l: LoadMultipleInst => processLoadStore(l, builder)
      case l: StoreInst => processLoadStore(l, builder)
      case l: StoreMultipleInst => processLoadStore(l, builder)
    }
  }
  
  private def processLoadStore(inst: LoadInst, builder: IRBuilder[ARM]) = {
    import AddressingMode._
    val (addr, b) = inst.addressingMode match {
      case PostIndex => (inst.indexingOperand.base.get: Expr, builder)
      case PreIndex => {
        val ea: Expr = inst.indexingOperand.base.get
        (ea, builder.assign(inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand))  
      }
      case Regular => (inst.indexingOperand: Expr, builder)
    }
    val bb = b.load(inst, Reg(inst.dest), addr)
    inst.addressingMode match {
      case PostIndex =>
        bb.assign(inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand)
      case PreIndex | Regular => bb
    }
  }
  
  private def processLoadStore(inst: LoadMultipleInst, builder: IRBuilder[ARM]) = {
    // ARM usually uses POP PC as return 
    val sizeInBytes = wordSizeInBits / 8
    val base: Reg = Reg(inst.base.base.get)
    val load = inst.destList.foldLeft(builder) {
      case (b, reg) => {
        val stb = b.load(inst, Reg(reg), base)
        if (inst.preindex)
          stb.assign(inst, base, base + Const(sizeInBytes, base.sizeInBits))
        else
          stb
      }
    }
    if (inst.destList.contains(Register(Register.Id.PC, None)))
      load.ret(inst, Reg(Register.Id.PC))
    else
      load
  }
  
  private def processLoadStore(inst: StoreInst, builder: IRBuilder[ARM]) = {
    import AddressingMode._
    val (addr, b) = inst.addressingMode match {
      case PostIndex => (inst.indexingOperand.base.get: Expr, builder)
      case PreIndex => {
        val ea: Expr = inst.indexingOperand.base.get
        (ea, builder.assign(inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand))
      }
      case Regular => (inst.indexingOperand: Expr, builder)
    }
    val bb = b.store(inst, addr, inst.src)
    inst.addressingMode match {
      case PostIndex =>
        bb.assign(inst, Reg(inst.indexingOperand.base.get), inst.indexingOperand)
      case PreIndex | Regular => bb
    }
  }
  
  private def processLoadStore(inst: StoreMultipleInst, builder: IRBuilder[ARM]) = {
    val sizeInBytes = wordSizeInBits / 8
    val base = Reg(inst.base.base.get)
    inst.srcList.foldLeft(builder) {
      case (b, reg) => {
        val stb = b.store(inst, Reg(reg), base)
        if (inst.preindex)
          stb.assign(inst, base, base + Const(sizeInBytes, base.sizeInBits))
        else
          stb
      }
    }
  }
  
  private def toIR(inst: UnaryArithInst, builder: IRBuilder[ARM]) = {
    val lv = Reg(inst.dest)
    import edu.psu.ist.plato.kaiming.arm.Opcode.Mnemonic._
    inst.opcode.mnemonic match {
      case NOT => builder.assign(inst, lv, Const(0, inst.src.sizeInBits) - inst.src)
      case ADR => builder.assign(inst, lv, inst.src)
      case _ => Exception.unreachable()
    }
  }
  
  private def toIR(inst: LongMulInst, builder: IRBuilder[ARM]) = {
    val tmpVar = builder.ctx.getNewTempVar(64)
    builder.assign(inst, tmpVar, inst.srcLeft * inst.srcRight)
    .assign(inst, Reg(inst.destHi), tmpVar |< 31)
    .assign(inst, Reg(inst.destLow), tmpVar |> 31)
  }
  
  private def toIR(inst: BitfieldClearInst, builder: IRBuilder[ARM]) = {
    val lv = Reg(inst.dest)
    val low = inst.lsb.value
    val high = (inst.lsb.value + inst.width.value)
    builder.assign(inst, lv, ((lv |> low) uext high) :+ (lv |<  high))
  }
  
  private def toIR(inst: BitfieldInsertInst, builder: IRBuilder[ARM]) = {
    val low = inst.lsb.value
    val width = inst.width.value
    val high = inst.width.value + inst.lsb.value
    val lv = Reg(inst.dest)
    builder.assign(inst, lv, (lv |> low) :+ (inst.src |> width) :+ (lv |< high))
  }
  
  override def toIRStatements(inst: Entry[ARM],
      builder: IRBuilder[ARM]) = {
    inst.asInstanceOf[Instruction] match {
      case i: BinaryArithInst => toIR(i, builder)
      case i: UnaryArithInst => toIR(i, builder)
      case i: LongMulInst => toIR(i, builder)
      case i: ExtractInst => toIR(i, builder)
      case i: ExtensionInst => toIR(i, builder)
      case i: BranchInst => toIR(i, builder)
      case i: CompareInst => toIR(i, builder)
      case i: MoveInst => toIR(i, builder)
      case i: LoadStoreInst => toIR(i, builder)
      case i: BitfieldClearInst => toIR(i, builder)
      case i: BitfieldInsertInst => toIR(i, builder)
      case i: NopInst => builder
    }
  }

  
}
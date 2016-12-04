package edu.psu.ist.plato.kaiming.arm

import edu.psu.ist.plato.kaiming.utils.ParserTrait

import edu.psu.ist.plato.kaiming.Label
import edu.psu.ist.plato.kaiming.utils.Exception

object ARMParser extends ParserTrait {

  import fastparse.noApi._
  import White._
  
  private val imm: P[Immediate] = P("#" ~~ integer).map(Immediate.get(_))
  
  private val reg: P[Register] = P(enum(Register.Id).!) map {
      x => Register.get(x.toUpperCase)
    } 
    
  import enumeratum._ 
  sealed trait ShiftType extends EnumEntry
  object ShiftType extends Enum[ShiftType] {
    
    val values = findValues 
  
    case object ASR extends ShiftType
    case object LSL extends ShiftType
    case object ROR extends ShiftType
    case object LSR extends ShiftType
    case object RRX extends ShiftType
    
  }

  private val shiftType: P[ShiftType] = P(enum(ShiftType).!) map { 
      x => ShiftType.withName(x.toUpperCase)
    }
    
  private val shifted: P[Register] = P(reg ~ "," ~ shiftType ~ (imm ?)) map {
    case (reg, st, sh) => {
      val shiftv = (sh match {
        case Some(imm) => imm.value
        case None => 0
      }).toInt
      if (shiftv == 0)
        reg
      else
        st match {
          case ShiftType.ASR => Register(reg.id, Some(Asr(shiftv)))
          case ShiftType.LSL => Register(reg.id, Some(Lsl(shiftv)))
          case ShiftType.ROR => Register(reg.id, Some(Ror(shiftv)))
          case ShiftType.LSR => Register(reg.id, Some(Lsr(shiftv)))
          case ShiftType.RRX => Register(reg.id, Some(Rrx()))
        }
    }
  }
  
  private val mem: P[Memory] = P(("[" ~ reg ~ (("," ~ (((CharIn("-+").?.!) ~ (shifted | reg)) | imm)?) ~ "]")) | positive) map {
    case ((base: Register), someOff) =>
      someOff match {
        case None => Memory.get(base)
        case Some(offset) =>
          offset match {
            case (sign, (offvalue: Register)) => {
              val signoff = sign match {
                case "" => Positive
                case s => if (s == "-") Negative else Positive
              }
              Memory.get(base, offvalue, signoff)
            }
            case imm: Immediate => Memory.get(base, imm.value)
          }
      }
    case addr: Long => Memory.get(addr)
  }
  
  private val cond: P[Condition] = P(enum(Condition).!) map {
      x => Condition.withName(x.toUpperCase)
    }
  
  private val operand: P[(Operand, Boolean)] = P(((shifted | reg | mem) ~ ("!".?.!)) | imm) map {
    case imm: Immediate => (imm, false)
    case ((op: Operand), preidx: String) => (op, preidx == "!")
  }
  
  private val operands: P[(Vector[Operand], Boolean)] = P(operand ~ (("," ~ operand).rep)) map {
    case (fop, fcomp, tail) => tail.foldLeft((Vector[Operand](fop), fcomp)) { 
      case ((l, preidx), x) => (l :+ x._1, x._2 || preidx) 
    }
  }
      
  private val regRange: P[List[Register]] = P(("R" ~ dec ~ "-") ~ ("R" ~ dec)) map {
    case (from, to) => (from to to).map {
      i => Register.get("R"+i)
    }.toList
  }
  
  private val singleReg: P[List[Register]] = P(reg.map(List[Register](_)))
  
  private val reglistItem = regRange | singleReg
  
  private val reglist: P[List[Register]] = P("{" ~ reglistItem ~ ("," ~ reglistItem).rep ~ "}") map {
    case (r, rl) => (r::rl.toList).flatten
  }
  
  private val poppush: P[Instruction] = P(positive ~ StringInIgnoreCase("PUSH", "POP").! ~ reglist ~ newline) map {
    case (addr, rawcode, oplist) => Instruction.create(addr, Opcode(rawcode), oplist.toVector, false)
  }
  
  private val lsm: P[Instruction] =
    P(positive ~ (StringInIgnoreCase("LDM", "STM").! ~~ enum(LSMultipleMode).!) ~
    (reg) ~ ("!".?.!) ~ ("," ~ reglist) ~ newline) map {
      case (addr, rawcode, base, preidx, rlist) => 
        Instruction.create(addr, Opcode(rawcode._1 + rawcode._2), (base::rlist).toVector, preidx == "!")
  }
  
  private val ldrAsMove: P[Instruction] = P(positive ~ IgnoreCase("LDR").! ~ reg ~ "," ~ "=" ~ positive ~ newline) map {
    case (addr, rawcode, dest, imm) =>
      Instruction.create(addr, Opcode(rawcode.toUpperCase), Vector(dest, Immediate.get(imm)), false)
  }
  
  private val mnemonic: P[String] = P((Alpha.repX(1) ~~ Aldigit.repX ~~ ("." ~~ enum(Condition)).?).!)

  private val opcode: P[Opcode] = mnemonic map { case opcode => Opcode(opcode.toUpperCase) }

  private val inst: P[Instruction] = P(positive ~ opcode ~ (operands.?) ~ newline) map {
    case (addr, code, oplist) => oplist match {
      case None => Instruction.create(addr, code, Vector[Operand](), false)
      case Some(operands) => 
        Instruction.create(addr, code, operands._1.toVector, operands._2)
    }
  }
  
  private val funlabel: P[Label] = P(positive ~ label ~ newline) map { x => Label(x._2) }
  
  private val function: P[Function] = P(funlabel ~ ((inst | poppush | lsm | ldrAsMove).rep(1))) map {
    case (label, insts) => new Function(label, insts.toVector)
  }
   
  val binaryunit: Parser[Seq[Function]] = function.rep
}
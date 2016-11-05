package edu.psu.ist.plato.kaiming.arm

import scala.util.parsing.combinator.RegexParsers

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions.bufferAsJavaList

import scala.language.postfixOps

import edu.psu.ist.plato.kaiming.Label

import edu.psu.ist.plato.kaiming.utils.Exception
import edu.psu.ist.plato.kaiming.utils.ParserTrait

import scala.Ordering
import scala.Vector

object ARMParser extends RegexParsers with ParserTrait {
  
  override val whiteSpace = whitespaceWithoutNewline
  
  private val nl: Parser[String] = newline 
  
  private val dec: Parser[Long] = """\d+""".r ^^ 
    { s => java.lang.Long.parseLong(s, 10) }
  
  private val hex: Parser[Long] = """0x[\da-fA-F]+""".r ^^ 
    { s => java.lang.Long.parseLong(s.substring(2).toLowerCase, 16) }
  
  private val positive: Parser[Long] = hex | dec
  
  private val address = positive
  
  private val integer: Parser[Long] = opt("-") ~ positive ^^ {
    case Some(_) ~ positive => -positive
    case None ~ positive => positive
  }
  
  private val imm: Parser[Immediate] = "#" ~> integer ^^ {
    case integer => Immediate.get(integer)
  }
  
  private val label: Parser[String] =
    """[a-zA-Z_]([_\-@\.a-zA-Z0-9])*:""".r ^^ { 
      x => x.toString.substring(0, x.length - 1)
    }
  
  private val reg: Parser[Register] = 
    ("(?i)(" + 
        Register.Id.values.map(_.entryName).sorted(Ordering[String].reverse).mkString("|")
        + ")").r ^^ {
    case string => Register.get(string.toUpperCase)
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
    

  private val shiftType: Parser[ShiftType] =
    ("(?i)" + "(" + ShiftType.values.map(_.entryName).mkString("|") + ")").r ^^ { 
      x => ShiftType.withName(x.toUpperCase)
    }
    
  private val shifted: Parser[Register] = (reg <~ ",") ~ shiftType ~ (imm ?) ^^ {
    case reg ~ st ~ sh => {
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
  
  private val mem: Parser[Memory] = (("[" ~> reg ~ (("," ~> ((("""[\-\+]""".r ?) ~ (shifted | reg)) | imm)?) <~ "]")) | address) ^^ {
    case (base: Register) ~ someOff =>
      someOff match {
        case None => Memory.get(base)
        case Some(offset) =>
          offset match {
            case sign ~ (offvalue: Register) => {
              val signoff = sign match {
                case None => Positive
                case Some(s) => if (s == "-") Negative else Positive
              }
              Memory.get(base, offvalue, signoff)
            }
            case imm: Immediate => Memory.get(base, imm.value)
          }
      }
    case addr: Long => Memory.get(addr)
  }
  
  private val cond: Parser[Condition] =
    ("(?i)" + "(" + Condition.values.map(_.entryName).mkString("|") + ")").r ^^ { 
      x => Condition.withName(x.toUpperCase)
    }
  
  private val operand: Parser[(Operand, Boolean)] = (((shifted | reg | mem) ~ ("!" ?)) | imm) ^^ {
    case imm: Immediate => (imm, false)
    case (op: Operand) ~ (preidx: Option[_]) => (op, preidx.isDefined)
  }
  
  private val operands: Parser[(List[Operand], Boolean)] = operand ~ (("," ~> operand)*) ^^ {
    case head ~ tail => (head::tail).foldRight((List[Operand](), false)) { 
      case (x, (l, preidx)) => (x._1:: l, x._2 || preidx) 
    }
  }
      
  private val regRange: Parser[List[Register]] = ("R" ~> dec <~ "-") ~ ("R" ~> dec) ^^ {
    case from ~ to => (from to to).map {
      i => Register.get("R"+i)
    }.toList
  }
  
  private val singleReg: Parser[List[Register]] = reg ^^ {
    case r => List[Register](r)
  }
  
  private val reglistItem = regRange | singleReg
  
  private val reglist: Parser[List[Register]] = "{" ~> (reglistItem) ~ (("," ~> reglistItem) *) <~ "}" ^^ {
    case r ~ rl => (r::rl).flatten
  }
  
  private val poppush: Parser[Instruction] = address ~ ("(?i)PUSH|POP".r) ~ reglist <~ nl ^^ {
    case addr ~ rawcode ~ oplist => Instruction.create(addr, Opcode(rawcode), oplist.toVector, false)
  }
  
  private val lsm: Parser[Instruction] =
    address ~ ("(?i)(LDM|STM)(" + LSMultipleMode.values.map(_.entryName).mkString("|") + ")").r ~
    (reg) ~ ("!" ?) ~ ("," ~> reglist) <~ nl ^^ {
      case addr ~ rawcode ~ base ~ preidx ~ rlist => 
        Instruction.create(addr, Opcode(rawcode), (base::rlist).toVector, preidx.isDefined)
  }
  
  private val ldrAsMove: Parser[Instruction] = address ~ "(?i)LDR".r ~ (reg <~ ",") ~ ("=" ~> positive) <~ nl ^^ {
    case addr ~ rawcode ~ dest ~ imm =>
      Instruction.create(addr, Opcode(rawcode.toUpperCase), Vector(dest, Immediate.get(imm)), false)
  }
  
  private val mnemonic: Parser[String] = """(?i)[a-z]+([a-z\d])*""".r
  
  private val opcode: Parser[Opcode] = mnemonic ^^ { case opcode => Opcode(opcode.toUpperCase) }

  private val inst: Parser[Instruction] = address ~ opcode ~ (operands ?) <~ nl ^^ {
    case addr ~ code ~ oplist => oplist match {
      case None => Instruction.create(addr, code, Vector[Operand](), false)
      case Some(operands) => 
        Instruction.create(addr, code, operands._1.toVector, operands._2)
    }
  }
  
  private val funlabel: Parser[Label] = address ~> label <~ nl ^^ { 
    case label => Label(label)
  }
  
  private val function: Parser[Function] = funlabel ~ ((inst | poppush | lsm | ldrAsMove) +) ^^ {
    case label ~ insts => new Function(label, insts.toVector)
  }
   
  val binaryunit: Parser[List[Function]] = rep(function)
  
  @throws(classOf[edu.psu.ist.plato.kaiming.utils.ParsingException])
  def parseBinaryUnit(input: String): List[Function] =
    parseAll(binaryunit, if (input.endsWith("\n")) input else input + '\n') match {
      case Success(value, _) => value
      case failure: NoSuccess =>
        Exception.parseError(failure.msg + "\n" + failure.next.offset + " " + failure.next.pos)
    }
   
  @throws(classOf[edu.psu.ist.plato.kaiming.utils.ParsingException])
  def parseBinaryUnitJava(input: String): java.util.List[Function] = ListBuffer(parseBinaryUnit(input):_*)
}
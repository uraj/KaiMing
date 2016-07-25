package edu.psu.ist.plato.kaiming.arm

import scala.util.parsing.combinator.RegexParsers

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions.bufferAsJavaList

import scala.language.postfixOps

import edu.psu.ist.plato.kaiming.Label

import edu.psu.ist.plato.kaiming.exception.ParsingException
import edu.psu.ist.plato.kaiming.exception.UnreachableCodeException

import scala.Ordering
import scala.Vector

object ARMParser extends RegexParsers() {
  override val whiteSpace = """[\t \r]+""".r
  
  private def nl: Parser[String] = """\n+""".r 
  
  private def dec: Parser[Long] = """\d+""".r ^^ 
    { s => java.lang.Long.parseLong(s, 10) }
  
  private def hex: Parser[Long] = """0x[\da-fA-F]+""".r ^^ 
    { s => java.lang.Long.parseLong(s.substring(2).toLowerCase, 16) }
  
  private def positive: Parser[Long] = hex | dec
  
  private def address = positive
  
  private def integer: Parser[Long] = opt("-") ~ positive ^^ {
    case Some(_) ~ positive => -positive
    case None ~ positive => positive
  }
  
  private def imm: Parser[Immediate] = "#" ~> integer ^^ {
    case integer => Immediate(integer)
  }
  
  private def label: Parser[String] =
    """[a-zA-Z_]([_\-@\.a-zA-Z0-9])*:""".r ^^ { 
      x => x.toString.substring(0, x.length - 1)
    }
  
  private def reg: Parser[Register] = 
    ("(?i)(" + 
        Register.Id.values.map(_.entryName).sorted(Ordering[String].reverse).mkString("|")
        + ")").r ^^ {
    case string => Register.get(string.toUpperCase)
  }
  
  private object Shift {
    
    import enumeratum._ 
    sealed trait Type extends EnumEntry
    object Type extends Enum[Type] {
    
      val values = findValues 
    
      case object ASR extends Type
      case object LSL extends Type
      case object ROR extends Type
    
    }
    
  }

  private def shiftType: Parser[Shift.Type] =
    ("(?i)" + "(" + Shift.Type.values.map(_.entryName).mkString("|") + ")").r ^^ { 
      x => Shift.Type.withName(x.toUpperCase)
    }
    
  private def shifted: Parser[Register] = (reg <~ ",") ~ shiftType ~ integer ^^ {
    case reg ~ st ~ sh => sh match {
      case sh if sh == 0 => reg
      case _ => st match {
        case Shift.Type.ASR => Register(reg.id, Some(Asr(sh.toInt)))
        case Shift.Type.LSL => Register(reg.id, Some(Lsl(sh.toInt)))
        case Shift.Type.ROR => Register(reg.id, Some(Ror(sh.toInt)))
      }
    }
  }
  
  private def mem: Parser[Memory] = (("[" ~> reg ~ (("," ~> (imm | reg)?) <~ "]")) | address) ^^ {
    case (reg: Register) ~ someInt => someInt match {
      case None => Memory.get(reg)
      case Some(int: Immediate) => Memory.get(reg, int)
      case Some(off: Register) => Memory.get(reg, off)
      case _ => throw new UnreachableCodeException
    }
    case addr: Long => Memory.get(Immediate.get(addr))
  }
  
  private def cond: Parser[Condition] =
    ("(?i)" + "(" + Condition.values.map(_.entryName).mkString("|") + ")").r ^^ { 
      x => Condition.withName(x.toUpperCase)
    }
  
  private def operand: Parser[(Operand, Boolean)] = (((reg | mem | shifted) ~ ("!" ?)) | imm) ^^ {
    case imm: Immediate => (imm, false)
    case (op: Operand) ~ (preidx: Option[_]) => (op, preidx.isDefined)
  }
  
  private def operands: Parser[(List[Operand], Boolean)] = operand ~ (("," ~> operand)*) ^^ {
    case head ~ tail => (head::tail).foldRight((List[Operand](), false)) { 
      case (x, (l, preidx)) => (x._1:: l, x._2 || preidx) 
    }
  }
      
  private def regRange: Parser[List[Register]] = ("R" ~> dec <~ "-") ~ ("R" ~> dec) ^^ {
    case from ~ to => (from to to).map {
      i => Register.get("R"+i)
    }.toList
  }
  
  private def singleReg: Parser[List[Register]] = reg ^^ {
    case r => List[Register](r)
  }
  
  private def reglistItem = regRange | singleReg
  
  private def reglist: Parser[List[Register]] = "{" ~> (reglistItem) ~ (("," ~> reglistItem) *) <~ "}" ^^ {
    case r ~ rl => (r::rl).flatten
  }
  
  private def poppush: Parser[Instruction] = address ~ ("(?i)PUSH|POP".r) ~ reglist <~ nl ^^ {
    case addr ~ rawcode ~ oplist => Instruction.create(addr, Opcode(rawcode), oplist.toVector, None, false)
  }
  
  private def lsm: Parser[Instruction] =
    address ~ ("(?i)(LDM|STM)(" + LSMultipleMode.values.map(_.entryName).mkString("|") + ")").r ~
    (reg) ~ ("!" ?) ~ ("," ~> reglist) <~ nl ^^ {
      case addr ~ rawcode ~ base ~ preidx ~ rlist => 
        Instruction.create(addr, Opcode(rawcode), (base::rlist).toVector, None, preidx.isDefined)
  }
  
  private def ldrAsMove: Parser[Instruction] = address ~ "(?i)LDR".r ~ (reg <~ ",") ~ ("=" ~> positive) <~ nl ^^ {
    case addr ~ rawcode ~ dest ~ imm =>
      Instruction.create(addr, Opcode("MOV"), Vector(dest, Immediate(imm)), None, false)
  }
  
  private def mnemonic: Parser[String] = 
    ("""(?i)[a-z]+([a-z\d])*((\.(""" + Condition.values.map(_.entryName).mkString("|") + "))?)").r
  
  private def opcode: Parser[Opcode] = mnemonic ^^ {
    case opcode => Opcode(opcode.toUpperCase)
  }

  private def inst: Parser[Instruction] = address ~ opcode ~ (operands ?) ~ (("," ~> cond) ?) <~ nl ^^ {
    case addr ~ code ~ oplist ~ cond => oplist match {
      case None => Instruction.create(addr, code, Vector[Operand](), cond, false)
      case Some(operands) => 
        Instruction.create(addr, code, operands._1.toVector, cond, operands._2)
    }
  }
  
  private def funlabel: Parser[Label] = address ~> label <~ nl ^^ { 
    case label => Label(label)
  }
  
  private def function: Parser[Function] = funlabel ~ ((inst | poppush | lsm | ldrAsMove) *) ^^ {
    case label ~ insts => new Function(label, insts)
  }
   
  def binaryunit: Parser[List[Function]] = rep(function)
   
  def parseBinaryUnit(input: String): List[Function] = parseAll(binaryunit, input) match {
    case Success(value, _) => value
    case failure: NoSuccess => throw new ParsingException(failure.msg + "\n" + failure.next.offset + " " + failure.next.pos)
  }
   
  @throws(classOf[ParsingException])
  def parseBinaryUnitJava(input: String): java.util.List[Function] = ListBuffer(parseBinaryUnit(input):_*)
}
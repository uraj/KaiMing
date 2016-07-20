package edu.psu.ist.plato.kaiming.aarch64.parsing

import scala.util.parsing.combinator.RegexParsers
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions.bufferAsJavaList

import edu.psu.ist.plato.kaiming.Label
import edu.psu.ist.plato.kaiming.exception.ParsingException
import edu.psu.ist.plato.kaiming.exception.UnreachableCodeException

import edu.psu.ist.plato.kaiming.aarch64._

object ARMParser extends RegexParsers() {
  override val whiteSpace = """[\t \r]+""".r
  
  def nl: Parser[String] = """\n+""".r 
  
  def dec: Parser[Long] = """\d+""".r ^^ 
    { s => java.lang.Long.parseLong(s, 10) }
  
  def hex: Parser[Long] = """0x[\da-fA-F]+""".r ^^ 
    { s => java.lang.Long.parseLong(s.substring(2).toLowerCase(), 16) }
  
  def positive: Parser[Long] = hex | dec
  
  def address = positive
  
  def integer: Parser[Long] = opt("-") ~ positive ^^ {
    case Some(_) ~ positive => -positive
    case None ~ positive => positive
  }
  
  def imm: Parser[Immediate] = "#" ~> integer ^^ {
    case integer => Immediate(integer)
  }
  
  def label: Parser[String] =
    """[a-zA-Z_]([_\-@\.a-zA-Z0-9])*:""".r ^^ { 
      x => x.toString.substring(0, x.length() - 1)
    }
  
  def reg: Parser[Register] = 
    ("(?i)(" + 
        Register.Id.values.map(_.entryName).sorted(Ordering[String].reverse).mkString("|")
        + ")").r ^^ {
    case string => Register.get(string.toUpperCase())
  }
  
  def shiftType: Parser[Shift.Type] =
    ("(?i)" + "(" + Shift.Type.values.map(_.entryName).mkString("|") + ")").r ^^ { 
      x => Shift.Type.withName(x.toUpperCase())
    }
    
  def shifted: Parser[Register] = (reg <~ ",") ~ shiftType ~ integer ^^ {
    case reg ~ st ~ sh => sh match {
      case sh if sh == 0 => reg
      case _ => Register(reg.id, Some(Shift(st, sh.toInt)))
    }
  }
  
  def mem: Parser[Memory] = (("[" ~> reg ~ (("," ~> (imm | reg)?) <~ "]")) | address) ^^ {
    case (reg: Register) ~ someInt => someInt match {
      case None => Memory.get(reg)
      case Some(int: Immediate) => Memory.get(reg, int)
      case Some(off: Register) => Memory.get(reg, off)
      case _ => throw new UnreachableCodeException
    }
    case addr: Long => Memory.get(Immediate.get(addr))
  }
  
  def cond: Parser[Condition] = 
    ("(?i)" + "(" + Condition.values.map(_.entryName).mkString("|") + ")").r ^^ { 
      x => Condition.withName(x.toUpperCase())
    }
  
  def operand: Parser[(Operand, Boolean)] = (((reg | mem | shifted) ~ ("!" ?)) | imm) ^^ {
    case imm: Immediate => (imm, false)
    case (op: Operand) ~ (preidx: Option[_]) => (op, preidx.isDefined)
  }
  
  def operands: Parser[(List[Operand], Boolean)] = operand ~ (("," ~> operand)*) ^^ {
    case head ~ tail => (head::tail).foldRight((List[Operand](), false)){ 
      case (x, (l, preidx)) => (x._1:: l, x._2 || preidx) 
    }
  }
  
  def mnemonic: Parser[String] = 
    ("""(?i)[a-z]+([a-z\d])*((\.(""" + Condition.values.map(_.entryName).mkString("|") + "))?)").r
  
  def opcode: Parser[Opcode] = mnemonic ^^ {
    case opcode => Opcode(opcode.toUpperCase())
  }
  
  def inst: Parser[Instruction] = address ~ opcode ~ (operands ?) ~ (("," ~> cond) ?) <~ nl ^^ {
    case addr ~ code ~ oplist ~ cond => oplist match {
      case None => Instruction.create(addr, code, Vector[Operand](), cond, false)
      case Some(operands) => 
        Instruction.create(addr, code, operands._1.toVector, cond, operands._2)
    }
  }
  
  def funlabel: Parser[Label] = address ~ label <~ nl ^^ { 
    case addr ~ label => Label(label, addr)
  }
  
  def function: Parser[Function] = funlabel ~ (inst *) ^^ {
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
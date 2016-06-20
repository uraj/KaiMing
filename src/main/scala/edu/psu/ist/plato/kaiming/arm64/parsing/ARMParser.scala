package edu.psu.ist.plato.kaiming.arm64.parsing

import scala.util.parsing.combinator.RegexParsers
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions.bufferAsJavaList

import edu.psu.ist.plato.kaiming.Label
import edu.psu.ist.plato.kaiming.exception.ParsingException
import edu.psu.ist.plato.kaiming.exception.UnreachableCodeException

import edu.psu.ist.plato.kaiming.arm64._

object ARMParser extends RegexParsers() {
  override val whiteSpace = """[\t \r]+""".r
  
  def nl : Parser[String] = """\n+""".r 
  
  def dec : Parser[Long] = """\d+""".r ^^ 
    { s => java.lang.Long.parseLong(s, 10) }
  
  def hex : Parser[Long] = """0x[\da-fA-F]+""".r ^^ 
    { s => java.lang.Long.parseLong(s.substring(2).toLowerCase(), 16) }
  
  def positive : Parser[Long] = hex | dec
  
  def address = positive
  
  def integer : Parser[Long] = opt("-") ~ positive ^^ {
      case Some(_) ~ positive => -positive
      case None ~ positive => positive
    }
  
  def imm : Parser[Immediate] = "#" ~> integer ^^ {
      case integer => Immediate.getImmediate(integer)
    }
  
  def label : Parser[String] =
    """[a-zA-Z_]([_\-@\.a-zA-Z0-9])*:""".r ^^ { 
      x => x.toString.substring(0, x.length() - 1)
    }
  
  def reg : Parser[Register] = 
    ("(?i)(" + 
        Register.Id.values().map(_.name()).sorted(Ordering[String].reverse).mkString("|")
        + ")").r ^^ {
      Register.getRegister(_)
    }
  
  def shiftType : Parser[Register.Shift.Type] =
    ("(?i)" + "(" + Register.Shift.Type.values().map(_.name()).mkString("|") + ")").r ^^ { 
      x => Register.Shift.Type.valueOf(x.toUpperCase())
    }
    
  def shifted : Parser[Register] = (reg <~ ",") ~ shiftType ~ integer ^^ {
      case reg ~ st ~ sh => sh match {
        case sh if sh == 0 => reg
        case _ => Register.getRegister(reg.id, new Register.Shift(st, sh.toInt))
      }
    }
  
  def mem : Parser[Memory] = (("[" ~> reg ~ (("," ~> (imm | reg)?) <~ "]")) | address) ^^ {
      case (reg : Register) ~ someInt => someInt match {
        case None => new Memory(reg)
        case Some(int : Immediate) => new Memory(reg, int.getValue())
        case Some(off : Register) => new Memory(reg, off)
        case _ => throw new UnreachableCodeException
      }
      case addr : Long => new Memory(null, addr)
    }
  
  def cond : Parser[Condition] = 
    ("(?i)" + "(" + Condition.values().map(_.name()).mkString("|") + ")").r ^^ { 
      x => Condition.valueOf(x.toUpperCase())
    }
  
  def operand : Parser[Operand] = reg | mem | shifted | imm
  
  def operands : Parser[List[Operand]] = operand ~ (("," ~> operand)*) ^^ {
    case head ~ tail => head::tail
  }
  
  def mnemonic : Parser[String] = 
    ("""(?i)[a-z]+([a-z\d])*((\.(""" + Condition.values().map(_.name()).mkString("|") + "))?)").r
  
  def opcode : Parser[Opcode] = mnemonic ^^ {
      case opcode => new Opcode(opcode)
    }
  
  def inst : Parser[Instruction] = address ~ opcode ~ (operands ?) ~ (cond ?) <~ nl ^^ {
    case addr ~ code ~ oplist ~ cond => oplist match {
      case None => Instruction.create(addr, code, Array[Operand](), cond.orNull)
      case Some(operands) => 
        Instruction.create(addr, code, operands.toArray[Operand], cond.orNull)
    }
  }
  
  def funlabel : Parser[Label] = address ~ label <~ nl ^^ { 
      case addr ~ label => new Label(label, addr)
    }
  
  def function : Parser[Function] = funlabel ~ (inst *) ^^ {
      case label ~ insts => new Function(label, ListBuffer(insts: _*))
    }
   
   def binaryunit : Parser[List[Function]] = rep(function)
   
   def parseBinaryUnit(input : String) : List[Function] = parseAll(binaryunit, input) match {
     case Success(value, _) => value
     case failure : NoSuccess => throw new ParsingException(failure.msg + "\n" + failure.next.offset + " " + failure.next.pos)
   }
   
   @throws(classOf[ParsingException])
   def parseBinaryUnitJava(input : String) : java.util.List[Function] = ListBuffer(parseBinaryUnit(input):_*)
}
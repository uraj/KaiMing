package edu.psu.ist.plato.kaiming.x86.parsing

import scala.util.parsing.combinator.RegexParsers
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions.bufferAsJavaList

import edu.psu.ist.plato.kaiming.Label;
import edu.psu.ist.plato.kaiming.exception.ParsingException;
import edu.psu.ist.plato.kaiming.x86.Function;
import edu.psu.ist.plato.kaiming.x86.Opcode;
import edu.psu.ist.plato.kaiming.x86.Instruction;
import edu.psu.ist.plato.kaiming.x86.Operand;
import edu.psu.ist.plato.kaiming.x86.Register;
import edu.psu.ist.plato.kaiming.x86.Immediate;
import edu.psu.ist.plato.kaiming.x86.Memory;

import edu.psu.ist.plato.kaiming.util.Tuple;

object GASParser extends RegexParsers() {
  
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
  
  def imm : Parser[Immediate] = "$" ~> integer ^^ {
      case integer => Immediate.getImmediate(integer)
    }
  
  def label : Parser[String] =
    """[a-zA-Z_]([_\-@\.a-zA-Z0-9])*:""".r ^^ { 
      x => x.toString.substring(0, x.length() - 1)
    }
  
  def funlabel : Parser[Label] = address ~ label ~ nl ^^ { 
      case addr ~ label ~ _ => new Label(label, addr)
    }
  
  
  def reg : Parser[Register] = 
    ("(?i)%" + "(" + Register.Id.values().map(_.name()).mkString("|") + ")").r ^^ {
      x => Register.getRegister(x.substring(1))
    }
  
  def sreg : Parser[Register] = """%[gesd]s""".r ^^ {
      x => Register.getRegister(x.substring(1))
    }

  // segment:displacement(base register, offset register, scalar multiplier)
  def membase1 : Parser[(Option[Register], Option[Register], Long)] = 
    "(" ~> (reg?) ~ (("," ~> reg ~ (("," ~> positive)?))?) <~ ")" ^^ {
      case base ~ offAndMulti =>
        offAndMulti match {
        case None => (base, None, 1)
        case Some(off ~ y) => y match {
          case None => (base, Some(off), 1)
          case Some(multi) => (base, Some(off), multi)
        }
      }
    }
    
  def membase2 : Parser[(Option[Register], Option[Register], Long)] =
    "(" ~> reg ~ ("," ~> positive) <~ ")" ^^ {
      case reg ~ positive => (None, Some(reg), positive)
    }
    
  def membase = membase1 | membase2
  
  def memdist : Parser[(Option[Register], Int)] =
    ((sreg <~ ":" ~ integer) | (sreg <~ ":") | integer) ^^ {
      case (sreg : Register) ~ (dist : Int) => (Some(sreg), dist)
      case sreg : Register => (Some(sreg), 0)
      case dist : Int => (None, dist)
    }
  
  def constructMem(memdist: (Option[Register], Int),
                   membase : (Option[Register], Option[Register], Int)) : Memory =
    memdist match {
      case (seg, dist) =>
        membase match {
          case (base, off, multi) => new Memory(seg.orNull, dist, base.orNull, off.orNull, multi)
        }
    }
  
  def mem : Parser[Memory] = (memdist ~ membase | memdist | membase) ^^ {
      case (memdist ~ membase) =>
        constructMem(memdist.asInstanceOf[(Option[Register], Int)],
                     membase.asInstanceOf[(Option[Register], Option[Register], Int)])
      case memdist : (_, _)  =>
        constructMem(memdist.asInstanceOf[(Option[Register], Int)], (None, None, 1))
      case membase : (_, _, _) =>
        constructMem((None, 0), membase.asInstanceOf[(Option[Register], Option[Register], Int)])
    }
    
  def operand : Parser[Operand] = mem | imm | reg
  
  def operands : Parser[List[Operand]] = operand ~ (("," ~> operand)*) ^^ {
      case first ~ rest => first :: rest
    }
  
  def prefix : Parser[String] = """(?i)rep(n?[ez])?|lock""".r ^^ { 
      _.toLowerCase()
    }
  
  def mnemonic : Parser[String] = """[a-zA-Z]+([a-zA-Z\d])*""".r 
  
  def opcode : Parser[Opcode] = (prefix?) ~ mnemonic ^^ {
      case None ~ opcode => new Opcode(opcode.toLowerCase())
      case Some(prefix) ~ opcode => new Opcode(prefix, opcode.toLowerCase())
    }
  
  
  def inst : Parser[Instruction] =
    address ~ opcode ~ ((("*"?) ~ operands)?) ~ nl ^^ {
      case addr ~ op ~ oplist ~ nl =>
        var indirect = false;
        var operands : Array[Operand] = null;
        oplist match {
          case None => operands = new Array[Operand](0);
          case Some(star ~ list) =>
            operands = list.toArray
            star match {
              case None =>
              case Some(_) => indirect = true 
            }
        }
        Instruction.create(addr, op, operands, indirect)
    }
  
  def function : Parser[Function] = funlabel ~ rep(inst) ^^ {
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
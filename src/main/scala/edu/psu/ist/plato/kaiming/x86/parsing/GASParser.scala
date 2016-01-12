package edu.psu.ist.plato.kaiming.x86.parsing

import scala.util.parsing.combinator.RegexParsers
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions.bufferAsJavaList

import edu.psu.ist.plato.kaiming.x86.AsmLabel;
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
  
  private def str2int(str : String, radix : Int) : Int = {
    var ret = 0
    var base = 1
    for (i <- str.reverse) {
      if ('0' <= i && i <= '9') {
        val digit = i - '0'
        if (digit >= radix)
          throw new NumberFormatException(str)
        ret += digit * base
      } else if ('a' <= i && i <= 'z') {
        val digit = i - 'a' + 10
        if (digit >= radix)
          throw new NumberFormatException(str)
        ret += digit * base
      } else {
        throw new NumberFormatException(str)
      }
      base *= radix
    }
    return ret
  }
  
  def dec : Parser[Int] = """\d+""".r ^^ 
    { s => str2int(s, 10) }
  
  def hex : Parser[Int] = """0x[\da-fA-F]+""".r ^^ 
    { s => str2int(s.substring(2).toLowerCase(), 16) }
  
  def positive : Parser[Int] = hex | dec
  
  def address = positive
  
  def integer : Parser[Int] = opt("-") ~ positive ^^ {
      case Some(_) ~ positive => positive
      case None ~ positive => -positive
    }
  
  def imm : Parser[Immediate] = "$" ~> integer ^^ {
      case integer => Immediate.getImmediate(integer)
    }
  
  def label : Parser[String] =
    """[a-zA-Z_]([_\-@\.a-zA-Z0-9])*:""".r ^^ { 
      x => x.toString.substring(0, x.length() - 1)
    }
  
  def funlabel : Parser[AsmLabel] = address ~ label ~ nl ^^ { 
      case addr ~ label ~ _ => new AsmLabel(label, addr)
    }
  
  
  def reg : Parser[Register] = 
    """%(e?(ax|bx|cx|dx|si|di|sp|bp|ip|iz)|[abcd][hl])""".r ^^ {
      x => Register.getRegister(x.substring(1))
    }
  
  def sreg : Parser[Register] = """%[gesd]s""".r ^^ {
      x => Register.getRegister(x.substring(1))
    }

  // segment:displacement(base register, offset register, scalar multiplier)
  def membase : Parser[(Option[Register], Option[Register], Int)] = 
    "(" ~> reg ~ (("," ~> reg ~ (("," ~> positive)?))?) <~ ")" ^^ {
      case base ~ offAndMulti => offAndMulti match {
        case None => (Some(base), None, 1)
        case Some(off ~ y) => y match {
          case None => (Some(base), Some(off), 1)
          case Some(multi) => (Some(base), Some(off), multi)
        }
      }
    }
  
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
      case first ~ (rest : List[Operand] @unchecked) => first :: rest
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
        Instruction.createInstruction(addr, op, operands, indirect)          
    }
  
  def function : Parser[Function] = funlabel ~ rep(inst) ^^ {
      case label ~ insts => new Function(label, ListBuffer(insts: _*))
    }
   
   def binaryunit : Parser[List[Function]] = rep(function)
   
   def parseBinaryUnit(input : String) : List[Function] = parseAll(binaryunit, input) match {
     case Success(value, _) => value
     case failure : NoSuccess => println(failure.next.first); println(failure.next.offset); scala.sys.error(failure.msg)
   }

}

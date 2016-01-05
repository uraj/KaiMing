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

class Parser extends RegexParsers() {
  
  override val whiteSpace = """[\t\f]+""".r
  
  def nl : Parser[Any] = elem('\n')
  
  private def str2int(str : String, radix : Int) : Int = {
    var ret = 0
    var base = 1
    for (i <- str.reverse) {
      if ('0' <= i && i <= '9') {
        val digit = 'i' - '0'
        if (digit >= radix)
          throw new NumberFormatException(str)
        ret += digit * base
      } else if ('a' <= i && i <= 'z') {
        val digit = 'i' - 'a'
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
  
  def integer : Parser[Int] = opt(elem('-')) ~ positive ^^ {
      case Some(_) ~ positive => positive
      case None ~ positive => -positive
    }
  
  def imm : Parser[Immediate] = elem('$') ~ integer ^^ {
      case _ ~ integer => Immediate.getImmediate(integer)
    }
  
  def label : Parser[String] =
    """[a-zA-Z_]([_-@\.a-zA-Z0-9])*:""".r ^^ { _.toString }
  
  def funlabel : Parser[AsmLabel] = address ~ label ~ nl ^^ { 
      case addr ~ label ~ _ => new AsmLabel(label, addr)
    }
  
  
  def reg : Parser[Register] = 
    """%(e?(ax|bx|cx|dx|si|di|sp|bp|ip|iz)|[abcd][hl])""".r ^^ {
      x => Register.getRegister(x.substring(1))
    }
  
  def sreg : Parser[Register] = """%([gesd]s)""".r ^^ {
      x => Register.getRegister(x.substring(1))
    }
  // segment:displacement(base register, offset register, scalar multiplier)
  def membase : Parser[(Option[Register], Option[Register], Int)] = 
    '(' ~ reg ~ opt(',' ~ reg ~ opt(',' ~ positive)) ~ ')' ^^ {
      case '(' ~ base ~ x ~ ')' => x match {
        case None => (Some(base), None, 1)
        case Some(',' ~ off ~ y) => y match {
          case None => (Some(base), Some(off), 1)
          case Some(',' ~ multi) => (Some(base), Some(off), multi)
        }
      }
    }
  
  def memdist : Parser[(Option[Register], Int)] = opt(sreg ~ ':') ~ opt(integer) ^^ {
      case seg ~ dist => var k = 0; dist match {
        case None =>
        case Some(int) => k = int
      }; seg match {
        case None => (None, k)
        case Some(reg ~ ':') => (Some(reg), k)
      }
    }
  
  def mem : Parser[Memory] = memdist ~ membase ^^ {
      case ((seg, dist)) ~ ((base, off, multi)) =>
        new Memory(seg.orNull, dist, base.orNull, off.orNull, multi)
    }
  
  
  def operand : Parser[Operand] = mem | imm | reg
  
  def prefix : Parser[String] = """(?i)rep(n?[ez])?|lock""".r ^^ { 
      _.toString().toLowerCase()
    }
  
  def opcode : Parser[Opcode] = opt(prefix) ~ """[a-zA-Z])+([a-zA-Z\d])*""".r ^^ {
      case None ~ opcode => new Opcode(opcode)
      case Some(prefix) ~ opcode => new Opcode(prefix, opcode)
    }
  
  def inst : Parser[Instruction] =
    address ~ opcode ~ opt('*') ~ opt(rep1(operand, ',' ~ operand)) ^^ {
      case addr ~ op ~ indirect ~ operands =>
        Instruction.createInstruction(
            addr,
            op, 
            operands match {
              case None => new Array[Operand](0)
              case Some(list : List[Operand] @unchecked) => list.toArray
            },
            indirect match {
              case None => false
              case Some(_) => true
            })
    }
  
   def function : Parser[Function] = funlabel ~ rep(inst) ^^ {
       case label ~ insts => new Function(label, ListBuffer(insts: _*))
     }
   
   def binaryunit : Parser[List[Function]] = rep(function)

}
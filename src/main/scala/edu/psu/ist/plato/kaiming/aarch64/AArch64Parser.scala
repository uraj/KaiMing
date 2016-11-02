package edu.psu.ist.plato.kaiming.aarch64

import scala.util.parsing.combinator.RegexParsers

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions.bufferAsJavaList

import scala.language.postfixOps

import edu.psu.ist.plato.kaiming.Label

import edu.psu.ist.plato.kaiming.utils.Exception
import edu.psu.ist.plato.kaiming.utils.ParserTrait

import java.io.File
import scala.io.Source

import scala.Ordering
import scala.Vector

object AArch64Parser extends RegexParsers with ParserTrait {
  
  override val whiteSpace = whitespaceWithoutNewline

  private def nl: Parser[Any] = newline | EOI
  
  private def dec: Parser[Long] = """\d+""".r ^^ 
    { s => java.lang.Long.parseLong(s, 10) }
  
  private def hex: Parser[Long] = """0x[\da-fA-F]+""".r ^^ 
    { s => parseInteger(s.substring(2), 16) }
  
  private def positive: Parser[Long] = hex | dec
  
  private def address = positive
  
  private def integer: Parser[Long] = ("-" ?) ~ positive ^^ {
    case Some(_) ~ positive => -positive
    case None ~ positive => positive
  }
  
  private val plainImm: Parser[Immediate] = "#" ~> integer ^^ { Immediate(_) }
  
  private val shiftedImm: Parser[Immediate] = ("#" ~> integer <~ ",") ~ ("lsl" ~> "#" ~> integer) ^^ {
    case base ~ shift =>
      if(shift % 16 == 0)
        Immediate(base, shift.toInt)
      else
        Immediate(base, shift.toInt)
  }
  
  private val imm: Parser[Immediate] = plainImm | shiftedImm 
  
  private val plainLabel: Parser[String] =
    """[a-zA-Z_]([_\-@\.a-zA-Z0-9])*:""".r ^^ { 
      x => x.substring(0, x.length() - 1)
    }
  
  private val quotedLabel: Parser[String] =
    """\".+\":""".r ^^ {
      x => x.substring(1, x.length() - 2)
    }
  
  private val label = plainLabel | quotedLabel
  
  private val reg: Parser[Register] = 
    ("(?i)(" + 
        Register.Id.values.map(_.entryName).sorted(Ordering[String].reverse).mkString("|")
        + ")").r ^^ {
    case string => Register.get(string.toUpperCase())
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

  private val shiftType: Parser[Shift.Type] =
    ("(?i)" + "(" + Shift.Type.values.map(_.entryName).mkString("|") + ")").r ^^ { 
      x => Shift.Type.withName(x.toUpperCase())
    }
    
  private val shifted: Parser[ShiftedRegister] = (reg <~ ",") ~ shiftType ~ integer ^^ {
    case reg ~ st ~ sh => st match {
      case Shift.Type.ASR => ShiftedRegister(reg, if (sh == 0) None else Some(Asr(sh.toInt)))
      case Shift.Type.LSL => ShiftedRegister(reg, if (sh == 0) None else Some(Lsl(sh.toInt)))
      case Shift.Type.ROR => ShiftedRegister(reg, if (sh == 0) None else Some(Ror(sh.toInt)))
    }
  }
  
  private val mem: Parser[Memory] = (("[" ~> reg ~ (("," ~> (shifted | imm | reg)?) <~ "]")) | address) ^^ {
    case (reg: Register) ~ someInt => someInt match {
      case None => Memory.get(reg)
      case Some(int: Immediate) => Memory.get(reg, int)
      case Some(off: ShiftedRegister) => Memory.get(reg, off)
      case Some(reg: Register) => Memory.get(reg) 
      case _ => Exception.unreachable()
    }
    case addr: Long => Memory.get(Immediate(addr))
  }
  
  private val cond: Parser[Condition] =
    ("(?i)" + "(" + Condition.values.map(_.entryName).mkString("|") + ")").r ^^ { 
      x => Condition.withName(x.toUpperCase())
    }
  
  private val operand: Parser[(Operand, Boolean)] = (((shifted | reg | mem) ~ ("!" ?)) | imm) ^^ {
    case imm: Immediate => (imm, false)
    case (op: Operand) ~ (preidx: Option[_]) => (op, preidx.isDefined)
  }
  
  private val operands: Parser[(List[Operand], Boolean)] = operand ~ (("," ~> operand)*) ^^ {
    case head ~ tail => (head::tail).foldRight((List[Operand](), false)){ 
      case (x, (l, preidx)) => (x._1:: l, x._2 || preidx) 
    }
  }
  
  private val mnemonic: Parser[String] = 
    ("""(?i)[a-z]+([a-z\d])*((\.(""" + Condition.values.map(_.entryName).mkString("|") + "))?)").r
  
  private val opcode: Parser[Opcode] = mnemonic ^^ {
    case opcode => Opcode(opcode.toUpperCase())
  } ^? (
    { case opcode if opcode.mnemonic != Opcode.OpClass.Unsupported => opcode },
    s => s"Unsupported opcode: ${s.rawcode}"
  )
  
  private val inst: Parser[Instruction] = address ~ opcode ~ (operands ?) ~ (("," ~> cond) ?) <~ nl ^^ {
    case addr ~ code ~ oplist ~ cond => oplist match {
      case None => Instruction.create(addr, code, Vector[Operand](), cond, false)
      case Some(operands) => 
        Instruction.create(addr, code, operands._1.toVector, cond, operands._2)
    }
  }
  
  private val funlabel: Parser[Label] = address ~> label <~ nl ^^ { 
    case label => Label(label)
  }
  
  private val function: Parser[Function] = funlabel ~ (inst *) ^^ {
    case label ~ insts => new Function(label, insts)
  }
  
  private val funlabelLine: Parser[Either[Label, Instruction]] = funlabel ^^ { 
    case label => Left[Label, Instruction](label)
  }
  
  private val instLine: Parser[Either[Label, Instruction]] = inst ^^ {
    case inst => Right(inst)
  }
  
  private val singleLine = (funlabelLine | instLine)
   
  val binaryunit: Parser[List[Function]] = function *
  
  @throws(classOf[edu.psu.ist.plato.kaiming.utils.ParsingException])
  def parseBinaryUnit(input: String): List[Function] = 
    parseAll(binaryunit, input) match {
      case Success(value, _) => value
      case failure: NoSuccess =>
        Exception.parseError(failure.msg + "\n" + failure.next.offset + " " + failure.next.pos)
    }

  @throws(classOf[edu.psu.ist.plato.kaiming.utils.ParsingException])
  def parseBinaryUnitJava(input: String): java.util.List[Function] = ListBuffer(parseBinaryUnit(input):_*)
  
  def parseFile(f: File) = {
    val result = 
      Source.fromFile(f).getLines.foldLeft(
          List[Function](), None: Option[(Label, List[Instruction])]) {
      case (prev, line) =>
        if (prev._2.isDefined || line.contains(':')) {
          parseAll(singleLine, line) match {
            case Success(value, input) if input.atEnd => value match {
              case Left(label) => prev._2 match {
                case None => (prev._1, Some(label, Nil))
                case Some((oldL, insts)) => (new Function(oldL, insts.reverse)::prev._1, None)
              }
              case Right(inst) => prev._2 match {
                case None => (prev._1, None)
                case Some((oldL, insts)) => (prev._1, Some((oldL, inst::insts)))
              }
            }
            case _ => (prev._1, None)
          }
        }
        else
          prev
      }
    result._2 match {
      case None => result._1
      case Some((label, insts)) => (new Function(label, insts.reverse))::result._1
    }
  }
}
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

  private val end: Parser[Any] = EOI | newline
  
  private val dec: Parser[Long] = """\d+""".r ^^ 
    { s => java.lang.Long.parseLong(s, 10) }
  
  private val hex: Parser[Long] = """0[xX][\da-fA-F]+""".r ^^ 
    { s => parseInteger(s.substring(2), 16) }
  
  private val positive: Parser[Long] = hex | dec
  
  private val address = positive
  
  private val integer: Parser[Long] = ("-" ?) ~ positive ^^ {
    case Some(_) ~ positive => -positive
    case None ~ positive => positive
  }
  
  private val plainImm: Parser[Immediate] = "#" ~> integer ^^ { Immediate(_) }
  
  private val shiftedImm: Parser[Immediate] = ("#" ~> integer <~ ",") ~ ("""(?i)lsl""".r ~> ("#" ~> integer)) ^^ {
    case base ~ shift =>
      if(shift % 16 == 0)
        Immediate(base, shift.toInt)
      else
        Immediate(base, shift.toInt)
  }
  
  private val imm: Parser[Immediate] = shiftedImm | plainImm 
  
  private val plainLabel: Parser[String] =
    """[a-zA-Z_]([_\-@\.a-zA-Z0-9])*:""".r ^^ { 
      x => x.substring(0, x.length - 1)
    }
  
  private val quotedLabel: Parser[String] =
    """\".+\":""".r ^^ {
      x => x.substring(1, x.length - 2)
    }
  
  private val label = plainLabel | quotedLabel
  
  private val reg: Parser[Register] = regexFromEnum(Register.Id) ^^ {
    case string => Register.get(string.toUpperCase)
  }

  private object RegExtension {
    
    import enumeratum._

    sealed trait Type extends EnumEntry
    object Type extends Enum[Type] {
    
      val values = findValues 
    
      case object ASR extends Type
      case object LSL extends Type
      case object LSR extends Type
      case object ROR extends Type
      
      case object UXTB extends Type
      case object UXTH extends Type
      case object UXTW extends Type
      case object UXTX extends Type
      case object SXTB extends Type
      case object SXTH extends Type
      case object SXTW extends Type
      case object SXTX extends Type
    }
    
  }

  private val regMod: Parser[RegModifier] = {
    import RegExtension.Type._
    (regexFromEnum(RegExtension.Type) ^^ {
      x => RegExtension.Type.withName(x.toUpperCase)
    }) ~ (("#" ~> integer)?) ^^ {
      case ext ~ int => ext match {
        case ASR => Asr(int.get.toInt)
        case LSL => Lsl(int.get.toInt)
        case LSR => Lsr(int.get.toInt)
        case ROR => Ror(int.get.toInt)
        case UXTB => Uxtb(int.getOrElse(0L).toInt)
        case UXTH => Uxth(int.getOrElse(0L).toInt)
        case UXTW => Uxtw(int.getOrElse(0L).toInt)
        case UXTX => Uxtx(int.getOrElse(0L).toInt)
        case SXTB => Sxtb(int.getOrElse(0L).toInt)
        case SXTH => Sxth(int.getOrElse(0L).toInt)
        case SXTW => Sxtw(int.getOrElse(0L).toInt)
        case SXTX => Sxtx(int.getOrElse(0L).toInt)
      }
    }
  }
    
  private val mreg: Parser[ModifiedRegister] = (reg <~ ",") ~ regMod ^^ {
    case reg ~ st => ModifiedRegister(reg, st)
  }
  
  private val mem: Parser[Memory] = (("[" ~> reg ~ (("," ~> (mreg | reg | plainImm)?) <~ "]")) | address) ^^ {
    case (reg: Register) ~ someInt => someInt match {
      case None => Memory.get(reg)
      case Some(int: Immediate) => Memory.get(reg, int)
      case Some(off: ModifiedRegister) => Memory.get(reg, off)
      case Some(reg: Register) => Memory.get(reg) 
      case _ => Exception.unreachable()
    }
    case addr: Long => Memory.get(Immediate(addr))
  }
  
  private val cond: Parser[Condition] =
    ("(?i)" + "(" + Condition.values.map(_.entryName).mkString("|") + ")").r ^^ { 
      x => Condition.withName(x.toUpperCase)
    }
  
  private val operand: Parser[(Operand, Boolean)] = (((mreg | reg | mem) ~ ("!" ?)) | imm) ^^ {
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
    case opcode => Opcode.get(opcode.toUpperCase)
  } ^? (
    { case Left(opcode) => opcode },
    s => s"Unsupported opcode: ${s.right.get}"
  )
  
  private val inst: Parser[Instruction] = (address ~ opcode ~ (operands ?) ~ (("," ~> cond) ?)) <~ end ^^ {
    case addr ~ code ~ oplist ~ cond => oplist match {
      case None => Instruction.create(addr, code, Vector[Operand](), cond, false)
      case Some(operands) => 
        Instruction.create(addr, code, operands._1.toVector, cond, operands._2)
    }
  }
  
  private val funlabel: Parser[Label] = address ~> label <~ end ^^ { 
    case label => Label(label)
  }
  
  private val function: Parser[Function] = funlabel ~ (inst *) ^^ {
    case label ~ insts => new Function(label, insts.toVector)
  }
  
  @inline
  private val funlabelLine: Parser[Either[Label, Instruction]] = funlabel ^^ { 
    case label => Left[Label, Instruction](label)
  }
  
  @inline
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
  
  def parseFile(f: File) = new Iterator[(Function, Boolean)]() {
    val lines = Source.fromFile(f).getLines()
    
    var _cache: Option[(Function, Boolean)] = None
    var _label: Option[Label] = None
    lines.find {
        x => parseAll(singleLine, x) match {
          case Success(Left(label), input) => 
            _label = Some(label); true
          case _ => false
        }
      }
    
    private def tryNext: Unit = {
      if (_label.isDefined) {
        val label = _label.get
        _label = None
        var complete = true
        var insts = List[Instruction]()
        lines.find {
          x => parseAll(singleLine, x) match {
            case Success(value, input) => value match {
              case Left(label) => _label = Some(label); true
              case Right(inst) => insts = inst::insts; false
            }
            case _ => complete = false; false
          }
        }
        if (insts.size > 0)
          _cache = Some(new Function(label, insts.reverse.toVector), complete)
        else
          tryNext
      }
    }
    
    def hasNext =  _cache.isDefined || { tryNext; _cache.isDefined }
    
    def next = {
      if (hasNext) {
        val ret = _cache.get
        _cache = None
        ret
      } else {
        throw new NoSuchElementException("next on empty iterator")
      }
    }
  }
}
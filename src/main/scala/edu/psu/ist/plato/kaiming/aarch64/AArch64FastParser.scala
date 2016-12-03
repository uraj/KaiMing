package edu.psu.ist.plato.kaiming.aarch64

import java.io.File
import scala.io.Source

import edu.psu.ist.plato.kaiming.utils.FastParserTrait
import edu.psu.ist.plato.kaiming.utils.Exception

import edu.psu.ist.plato.kaiming.Label

object AArch64FastParser extends FastParserTrait {
  
  val White = fastparse.WhitespaceApi.Wrapper{
    import fastparse.all._
    NoTrace(CharIn(" \t\r").rep)
  }
  
  import fastparse.noApi._
  import White._
  
  private val newline: P[Unit] = P("\n".rep(1))
  
  private val end: P[Unit] = P(newline | End)
  
  private val dec: P[Long] = P(digit.repX(1).!).map(parseInteger(_, 10))
  
  private val hex: P[Long] =
    P("0" ~~ CharIn("xX") ~~ CharIn('0' to '9', 'a' to 'f', 'A' to 'F').repX(1).!).map(parseInteger(_, 16))
  
  
  private val positive: P[Long] = P(hex | dec)
  
  private val integer: P[Long] = P("-".?.! ~ positive) map { 
    case ("", positive) => positive
    case (_, positive) => -positive
  }
  
  private val plainImm: P[Immediate] = P("#" ~~ integer).map(Immediate(_))
  
  private val shiftedImm: P[Immediate] =
    P("#" ~ integer ~ "," ~ IgnoreCase("lsl") ~ "#" ~ integer) map {
      case (base, shift) =>
        if(shift % 16 == 0) Immediate(base, shift.toInt)
        else Immediate(base, shift.toInt)
    }
  
  private val imm: P[Immediate] = P(shiftedImm | plainImm)

  private val plainLabel: P[String] = P((Alpha ~~
        CharIn("_-@.", 'a' to 'z', 'A' to 'Z', '0' to '9').repX).! ~~ ":")
  
  private val quotedLabel: P[String] = P("\"" ~~ (!"\"" ~~ AnyChar).repX(1).! ~~ "\":")
  
  private val label = P(plainLabel | quotedLabel)
  
  private val reg: P[Register] = P(enum(Register.Id).!) map {
    x => Register.get(x.toUpperCase)
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
  
  private val regMod: P[RegModifier] = {
    import RegExtension.Type._
    P(((enum(RegExtension.Type) !) map {
      x => RegExtension.Type.withName(x.toUpperCase)
    }) ~ (("#" ~~ integer) ?)) map {
      case (ext, int) => ext match {
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
    
  private val mreg: P[ModifiedRegister] = P(reg ~ "," ~ regMod) map {
    case (reg, st) => ModifiedRegister(reg, st)
  }
  
  private val mem: P[Memory] = P(("[" ~ reg ~ (("," ~ (mreg | reg | plainImm)?) ~ "]")) | positive) map {
    case (reg: Register, someInt) => someInt match {
      case None => Memory.get(reg)
      case Some(int: Immediate) => Memory.get(reg, int)
      case Some(off: ModifiedRegister) => Memory.get(reg, off)
      case Some(reg: Register) => Memory.get(reg) 
      case _ => Exception.unreachable()
    }
    case addr: Long => Memory.get(Immediate(addr))
  }
  
  private val cond: P[Condition] = P(enum(Condition).!) map {
      x => Condition.withName(x.toUpperCase)
    }
  
  private val operand: P[(Operand, Boolean)] = P(((mreg | reg | mem) ~ ("!".! ?)) | imm) map {
    case imm: Immediate => (imm, false)
    case (op: Operand, preidx: Option[_]) => (op, preidx.isDefined)
  }
  
  private val operands: P[(Vector[Operand], Boolean)] = P(operand ~ (("," ~ operand).rep)) map {
    case (fop, fbool, list) => list.foldLeft((Vector[Operand](fop), fbool)) { 
      case ((l, preidx), (nop, nbool)) => (l :+ nop, nbool || preidx) 
    }
  }

  private val mnemonic: P[String] = P((Alpha.repX(1) ~~ Aldigit.repX ~~ ("." ~~ enum(Condition)).?).!)
  
  private val opcode: P[Opcode] = (mnemonic.map {
    case opcode => Opcode.get(opcode.toUpperCase)
  }) flatMap {
    case Left(opcode) => &(AnyChar ?).map(_ => opcode)
    case Right(s) => (!(AnyChar ?)).map(_ => null).opaque(s"Unsupported opcode: $s}")
  }
  
  val inst: P[Instruction] = P((positive ~ opcode ~ (operands ?) ~ (("," ~ cond) ?)) ~ end) map {
    case (addr, code, oplist, cond) => oplist match {
      case None => Instruction.create(addr, code, Vector[Operand](), cond, false)
      case Some(operands) => 
        Instruction.create(addr, code, operands._1.toVector, cond, operands._2)
    }
  }
  
  private val funlabel: P[Label] = P(positive ~ label ~ end) map {
    x => Label(x._2)
  }
  
  private val function: P[Function] = P(funlabel ~ inst.rep) map {
    case (label, insts) => new Function(label, insts.toVector)
  }
  
  @inline
  private val funlabelLine: P[Either[Label, Instruction]] = funlabel map { 
    case label => Left[Label, Instruction](label)
  }
  
  @inline
  private val instLine: P[Either[Label, Instruction]] = inst map {
    case inst => Right(inst)
  }
  
  val singleLine = P(funlabelLine | instLine | (positive map { case int => Right(UnsupportedInst(int)) }))
   
  val binaryunit: P[Seq[Function]] = function.rep
  
  @throws(classOf[edu.psu.ist.plato.kaiming.utils.ParsingException])
  def parseBinaryUnit(input: String): Seq[Function] = 
    binaryunit.parse(input) match {
      case Parsed.Success(value, _) => value
      case failure: Parsed.Failure => Exception.parseError(failure.msg)
    }

  def parseFile(f: File): Iterator[(Function, Boolean)] = parseLines(Source.fromFile(f).getLines)
  
  def parseLines(lines: Iterator[String]): Iterator[(Function, Boolean)] = 
    new Iterator[(Function, Boolean)]() {

      var _cache: Option[(Function, Boolean)] = None
      var _label: Option[Label] = None
      lines.find {
          x =>
            singleLine.parse(x) match {
            case Parsed.Success(Left(label), _) =>
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
            x => singleLine.parse(x) match {
              case Parsed.Success(value, input) if input == x.length => value match {
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
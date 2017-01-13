package edu.psu.ist.plato.kaiming.aarch64

import java.io.File
import scala.io.Source

import edu.psu.ist.plato.kaiming.utils.ParserTrait
import edu.psu.ist.plato.kaiming.utils.Exception

import edu.psu.ist.plato.kaiming.Label

object AArch64Parser extends ParserTrait {

  import fastparse.noApi._
  import White._
  
  private val plainImm: P[Immediate] = ("#" ~~ integer).map(Immediate(_))
  
  private val shiftedImm: P[Immediate] =
    ("#" ~ integer ~ "," ~ IgnoreCase("lsl") ~ "#" ~ integer) map {
      case (base, shift) =>
        if(shift % 16 == 0) Immediate(base, shift.toInt)
        else Immediate(base, shift.toInt)
    }
  
  private val imm: P[Immediate] = shiftedImm | plainImm
  
  private val reg: P[Register] = enum(Register.Id).! map {
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
    ((enum(RegExtension.Type).! map {
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
    
  private val mreg: P[ModifiedRegister] = (reg ~ "," ~ regMod) map {
    case (reg, st) => ModifiedRegister(reg, st)
  }
  
  private val mem: P[Memory] = (("[" ~ reg ~ (("," ~ (mreg | reg | plainImm)?) ~ "]")) | positive) map {
    case (reg: Register, someInt) => someInt match {
      case None => Memory.get(reg)
      case Some(int: Immediate) => Memory.get(reg, int)
      case Some(off: ModifiedRegister) => Memory.get(reg, off)
      case Some(reg: Register) => Memory.get(reg) 
      case _ => Exception.unreachable()
    }
    case addr: Long => Memory.get(Immediate(addr))
  }
  
  private val cond: P[Condition] = enum(Condition).! map {
      x => Condition.withName(x.toUpperCase)
    }
  
  private val operand: P[Operand] = mreg | reg | mem | imm
  
  private val mnemonic: P[String] = (Alpha.repX(1) ~~ Aldigit.repX ~~ ("." ~~ enum(Condition)).?).!
  
  private val opcode: P[Opcode] = (mnemonic.map {
    case opcode => Opcode.get(opcode.toUpperCase)
  }) flatMap {
    case Left(opcode) => &(AnyChar ?).map(_ => opcode)
    case Right(s) => (!(AnyChar ?)).map(_ => null).opaque(s"Unsupported opcode: $s}")
  }
  
  val inst: P[Instruction] = (hex ~ opcode ~ operand.rep(sep=",") ~ ("!".! | ("," ~ cond)).? ~ end) map {
    case (addr, code, oplist, trail) => trail match {
      case Some("!") => Instruction.create(addr, code, oplist.toVector, Condition.AL, true)
      case Some(cond: Condition) => Instruction.create(addr, code, oplist.toVector, cond, false)
      case _ => Instruction.create(addr, code, oplist.toVector, Condition.AL, false)
    }
  }
  
  private val funlabel: P[Label] = (hex ~ label ~ end) map {
    x => Label(x._2)
  }
  
  private val function: P[Function] = (funlabel ~ inst.rep) map {
    case (label, insts) => new Function(label, insts.toVector)
  }
  
  private val funlabelLine: P[Either[Label, Instruction]] = funlabel map { 
    case label => Left[Label, Instruction](label)
  }
  
  private val instLine: P[Either[Label, Instruction]] = inst map {
    case inst => Right(inst)
  }
  
  val singleLine = funlabelLine | instLine
   
  val binaryunit: P[Seq[Function]] = function.rep ~ End
  
  @throws(classOf[edu.psu.ist.plato.kaiming.utils.ParsingException])
  def parseBinaryUnit(input: String): Seq[Function] = 
    binaryunit.parse(input) match {
      case Parsed.Success(value, _) => value
      case failure: Parsed.Failure => Exception.parseError(failure.msg)
    }

  def parseFile(f: File): Iterator[(Function, Boolean)] = parseLines(Source.fromFile(f).getLines)
  
  def parseStream(s: java.io.InputStream) = parseLines(Source.fromInputStream(s).getLines())
  
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
              case Parsed.Success(value, input) => value match {
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
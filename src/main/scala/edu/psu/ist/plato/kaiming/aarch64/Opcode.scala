package edu.psu.ist.plato.kaiming.aarch64

import enumeratum._

object Opcode {
  
  // FIXME: It took me a long to consider how to handle
  // opcode parsing in a nice and clean way. At this point
  // the best option seems to be a two-level hierarchy that
  // contains several instruction groups, each of which
  // has a bunch of subtype opcode. Implementing this
  // with plain Scala is too wordy. We need to utilize
  // Scala metaprogramming, ideally macros. I don't have the
  // time to do it at this point though. Maybe it can be
  // refactored later.
  
  sealed trait Mnemonic extends EnumEntry {
    sealed abstract class Subtype(_variants: String*) extends EnumEntry {
      val variants: Set[String] = _variants.toSet + this.entryName
    }
  }
  
  object Mnemonic extends Enum[Mnemonic] {
    
    val values = findValues
    
    case object BinArith extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object ADD extends Subtype("ADDS")
        case object ADC extends Subtype("ADCS")
        case object SUB extends Subtype("SUBS")
        case object SBC extends Subtype("SBCS")
        case object MUL extends Subtype("MULS",
            "UMULL", "UMULH", "SMULL", "SMULH",
            "UMULLS", "UMULHS", "SMULLS", "SMULHS")
        case object SDIV extends Subtype("SDIVS")
        case object UDIV extends Subtype("UDIVS")
        case object ASR extends Subtype
        case object LSL extends Subtype
        case object LSR extends Subtype
        case object ORR extends Subtype
        case object EOR extends Subtype
        case object EON extends Subtype
        case object ORN extends Subtype
        case object AND extends Subtype("ANDS")
        case object BIC extends Subtype("BICS")
      }
    }
    
    case object PCRelative extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object ADR extends Subtype
        case object ADRP extends Subtype
      }
    }
    
    case object UnArith extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object NEG extends Subtype("NEGS")
        case object NGC extends Subtype("NGCS")
        case object MVN extends Subtype
      }
    }

    case object MADD extends Mnemonic
    case object MSUB extends Mnemonic
    case object MNEG extends Mnemonic
    case object LDR extends Mnemonic
    case object LDP extends Mnemonic
    case object STR extends Mnemonic
    case object STP extends Mnemonic
    case object STX extends Mnemonic
    case object STXP extends Mnemonic
    
    case object Compare extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object CMP extends Subtype
        case object TST extends Subtype
        case object CMN extends Subtype
      }
    }
    case object CondCompare extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object CCMP extends Subtype
        case object CCMN extends Subtype
      }
    }
    
    case object UnSel extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object CSET extends Subtype
        case object CSETM extends Subtype
      }
    }
    
    case object BinSel extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object CINV extends Subtype
        case object CNEG extends Subtype
        case object CINC extends Subtype
      }
    }
    
    case object TriSel extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object CSEL extends Subtype
        case object CSNEG extends Subtype
        case object CSINC extends Subtype
        case object CSINV extends Subtype
      }
    }

    case object Move extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object MOV extends Subtype
        case object MOVK extends Subtype
        case object MOVZ extends Subtype
        case object MOVN extends Subtype
      }
    }
    
    case object Branch extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object B extends Subtype
        case object BR extends Subtype
        case object RET extends Subtype
        case object BL extends Subtype
        case object BLR extends Subtype
      }
    }
    
    case object CompBranch extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object CBZ extends Subtype
        case object CBNZ extends Subtype

      }
    }

    case object TestBranch extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object TBZ extends Subtype
        case object TBNZ extends Subtype
      }
    }
    
    case object Nop extends Mnemonic
    case object EXT extends Mnemonic
    case object BFM extends Mnemonic
    
    case object DataProcess extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object RBIT extends Subtype
        case object REV extends Subtype
        case object REV16 extends Subtype
        case object REV32 extends Subtype
        case object CLZ extends Subtype
        case object CLS extends Subtype
      }
    }
    
    case object System extends Mnemonic {
      object Subtype extends Enum[Subtype] {
        val values = findValues
        case object CLREX extends Subtype
        case object SYS extends Subtype
        case object MSR extends Subtype
        case object HINT extends Subtype
        case object DSB extends Subtype
        case object DMB extends Subtype
        case object ISB extends Subtype
        case object SYSL extends Subtype
        case object MRS extends Subtype
      }
    }
    
    case object Unsupported extends Mnemonic
  }
  
  import Mnemonic._
  private val rawToMnem = Mnemonic.values.foldLeft(Map[String, Mnemonic]()) {
    (map, mnem) => map ++ (mnem match {
      case LDR =>
        List("LDR", "LDRH", "LDRB",
             "LDRSW", "LDRSH", "LDRSB",  // signed load  
             "LDUR", "LDURH", "LDURB", "LDURSW", "LDURSH", "LDURSB", // unscaled offsets
             "LDAR", "LDARH", "LDARB",  // load-acquire
             "LDXR", "LDXRH", "LDXRB",  // load exclusive
             "LDAXR", "LDXARH", "LDXARB"  // load-acquire exclusive
             )
      case LDP => List("LDP", "LDPSW", "LDXP")
      case STP => List("STP", "STPSW", "STXP")
      case STR =>
        List("STR", "STRH", "STRB",
             "STRSW", "STRSH", "STRSB", // signed store
             "STUR", "STURH", "STURB", "STURSW", "STURSH", "STURSB", // unscaled offset
             "STLR", "STLRH", "STLRB")  // store-release
      case STX =>
        List("STXR", "STXRH", "STXRB",  // store exclusive
             "STLXR", "STLXRH", "STLXRB"  // store exclusive
             )
      case STXP => List("STXP")
      case CompBranch => CompBranch.Subtype.values.map(_.variants).flatten
      case TestBranch => TestBranch.Subtype.values.map(_.variants).flatten
      case Branch => Branch.Subtype.values.map(_.variants).flatten
      case PCRelative => PCRelative.Subtype.values.map(_.variants).flatten
      case Compare => Compare.Subtype.values.map(_.variants).flatten
      case CondCompare => CondCompare.Subtype.values.map(_.variants).flatten
      case BinArith => BinArith.Subtype.values.map(_.variants).flatten
      case UnArith => UnArith.Subtype.values.map(_.variants).flatten
      case UnSel => UnSel.Subtype.values.map(_.variants).flatten
      case BinSel => BinSel.Subtype.values.map(_.variants).flatten
      case TriSel => TriSel.Subtype.values.map(_.variants).flatten
      case DataProcess => DataProcess.Subtype.values.map(_.variants).flatten
      case Move => Move.Subtype.values.map(_.variants).flatten
      case EXT => List("SXTW", "SXTH", "SXTB", "UXTW", "UXTH", "UXTB")
      case BFM => List("BFM", "SBFM", "UBFM", "BFI", "UBFX", "SBFX", "BFXIL", "UBFIZ", "SBFIZ")
      case MADD => List("MADD")
      case MSUB => List("MSUB")
      case MNEG => List("MNEG")
      case Nop => List("NOP")
      case System => System.Subtype.values.map(_.variants).flatten
      case Unsupported =>
        List("SCVTF", "MVN", "FMOV", "FCMP", "FCVT", "FMUL", "MOVI",
             "FADD", "FSUB", "UCVTF", "FCSEL", "FDIV", "FABS", "FCCMP", "FCVTZS",
             "FNEG", "FCVTZU", "FRINTX", "FRINTP", "FRINTM", "FRINTA", "FRINTI", "EXT", "FSQRT",
             "BIF", "BIT", "DUP", "INS", "FNMUL")
    }).map((_ -> mnem))
  }

}

case class Opcode(rawcode: String) {
  
  val mnemonic: Opcode.Mnemonic = {
    val lookup = rawcode.split("\\.")(0)
    if (Opcode.rawToMnem.contains(lookup))
      Opcode.rawToMnem(lookup)
    else {
      Opcode.Mnemonic.Unsupported
    }
  }
  
  def getCondition = {
    val parts = rawcode.split("\\.")
    assert(parts.length <= 2)
    if (parts.length == 2) {
      assert(parts(0).equals("B"))
      Condition.withName(parts(1))
    } else {
      Condition.AL
    }
  }
  
}
package edu.psu.ist.plato.kaiming.aarch64

import enumeratum._
import edu.psu.ist.plato.kaiming.utils.Exception

object Opcode {
  
  sealed trait OpClass extends EnumEntry {
    sealed abstract class Mnemonic(_variants: String*) extends EnumEntry {
      val variants: Set[String] = _variants.toSet + this.entryName
    }
    def Mnemonic: Enum[Mnemonic] = Exception.unsupported(s"Opcode class $this does not have mnemonics")
  }
  
  object OpClass extends Enum[OpClass] {
    
    val values = findValues
    
    case object BinArith extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object ADD extends Mnemonic("ADDS")
        case object ADC extends Mnemonic("ADCS")
        case object SUB extends Mnemonic("SUBS")
        case object SBC extends Mnemonic("SBCS")
        
        case object MUL extends Mnemonic
        case object UMULL extends Mnemonic
        case object UMULH extends Mnemonic
        case object SMULL extends Mnemonic
        case object SMULH extends Mnemonic
        
        case object MNEG extends Mnemonic
        case object UMNEGL extends Mnemonic
        case object SMNEGL extends Mnemonic
                  
        case object SDIV extends Mnemonic("SDIVS")
        case object UDIV extends Mnemonic("UDIVS")
        case object ASR extends Mnemonic
        case object LSL extends Mnemonic
        case object LSR extends Mnemonic
        case object ORR extends Mnemonic
        case object EOR extends Mnemonic
        case object EON extends Mnemonic
        case object ORN extends Mnemonic
        case object AND extends Mnemonic("ANDS")
        case object BIC extends Mnemonic("BICS")
      }
    }
    
    case object PCRelative extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object ADR extends Mnemonic
        case object ADRP extends Mnemonic
      }
    }
    
    case object UnArith extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object NEG extends Mnemonic("NEGS")
        case object NGC extends Mnemonic("NGCS")
        case object MVN extends Mnemonic
      }
    }
    
    case object TriArith extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object MADD extends Mnemonic
        case object MSUB extends Mnemonic
        case object SMADDL extends Mnemonic
        case object SMSUBL extends Mnemonic
        case object UMADDL extends Mnemonic
        case object UMSUBL extends Mnemonic
      }
    }
    
    case object Load extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object LDR extends Mnemonic
        case object LDRH extends Mnemonic
        case object LDRB extends Mnemonic
        case object LDRSW extends Mnemonic
        case object LDRSH extends Mnemonic
        case object LDRSB extends Mnemonic
        case object LDUR extends Mnemonic
        case object LDURH extends Mnemonic
        case object LDURB extends Mnemonic
        case object LDURSW extends Mnemonic
        case object LDURSH extends Mnemonic
        case object LDURSB extends Mnemonic
        case object LDAR extends Mnemonic
        case object LDARH extends Mnemonic
        case object LDARB extends Mnemonic
        case object LDXR extends Mnemonic
        case object LDXRH extends Mnemonic
        case object LDXRB extends Mnemonic
        case object LDAXR extends Mnemonic
        case object LDXARH extends Mnemonic
        case object LDXARB extends Mnemonic
      }
    }
    
    case object LoadPair extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object LDP extends Mnemonic
        case object LDPSW extends Mnemonic
        case object LDXP extends Mnemonic
        case object LDNP extends Mnemonic
      }

    }
    
    case object Store extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object STR extends Mnemonic
        case object STRH extends Mnemonic
        case object STRB extends Mnemonic
        case object STRSW extends Mnemonic
        case object STRSH extends Mnemonic
        case object STRSB extends Mnemonic
        case object STUR extends Mnemonic
        case object STURH extends Mnemonic
        case object STURB extends Mnemonic
        case object STURSW extends Mnemonic
        case object STURSH extends Mnemonic
        case object STURSB extends Mnemonic
        case object STLR extends Mnemonic
        case object STLRH extends Mnemonic
        case object STLRB extends Mnemonic
      }
    }
    
    case object StorePair extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object STP extends Mnemonic
        case object STNP extends Mnemonic
      }
    }
    
    case object StoreEx extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object STXR extends Mnemonic
        case object STXRH extends Mnemonic
        case object STXRB extends Mnemonic
        case object STLXR extends Mnemonic
        case object STLXRH extends Mnemonic
        case object STLXRB extends Mnemonic
      }
    }
    
    case object StoreExPair extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object STXP extends Mnemonic
      }
    }
    
    case object Compare extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object CMP extends Mnemonic
        case object TST extends Mnemonic
        case object CMN extends Mnemonic
      }
    }
    
    case object CondCompare extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object CCMP extends Mnemonic
        case object CCMN extends Mnemonic
      }
    }
    
    case object UnSel extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object CSET extends Mnemonic
        case object CSETM extends Mnemonic
      }
    }
    
    case object BinSel extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object CINV extends Mnemonic
        case object CNEG extends Mnemonic
        case object CINC extends Mnemonic
      }
    }
    
    case object TriSel extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object CSEL extends Mnemonic
        case object CSNEG extends Mnemonic
        case object CSINC extends Mnemonic
        case object CSINV extends Mnemonic
      }
    }

    case object Move extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object MOV extends Mnemonic
        case object MOVK extends Mnemonic
        case object MOVZ extends Mnemonic
        case object MOVN extends Mnemonic
      }
    }
    
    case object Branch extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object B extends Mnemonic
        case object BR extends Mnemonic
        case object RET extends Mnemonic
        case object BL extends Mnemonic
        case object BLR extends Mnemonic
      }
    }
    
    case object CompBranch extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object CBZ extends Mnemonic
        case object CBNZ extends Mnemonic
      }
    }

    case object TestBranch extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object TBZ extends Mnemonic
        case object TBNZ extends Mnemonic
      }
    }
    
    case object Nop extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object NOP extends Mnemonic
      }
    }
    
    case object Extend extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object SXTW extends Mnemonic
        case object SXTH extends Mnemonic
        case object SXTB extends Mnemonic
        case object UXTW extends Mnemonic
        case object UXTH extends Mnemonic
        case object UXTB extends Mnemonic
      }
    }
    case object BFMove extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object BFM extends Mnemonic
        case object SBFM extends Mnemonic
        case object UBFM extends Mnemonic
        
        case object BFI extends Mnemonic
        case object SBFX extends Mnemonic
        case object UBFX extends Mnemonic
        
        case object BFXIL extends Mnemonic
        case object UBFIZ extends Mnemonic
        case object SBFIZ extends Mnemonic
      }
    }
    
    case object DataProcess extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object RBIT extends Mnemonic
        case object REV extends Mnemonic
        case object REV16 extends Mnemonic
        case object REV32 extends Mnemonic
        case object CLZ extends Mnemonic
        case object CLS extends Mnemonic
      }
    }
    
    case object System extends OpClass {
      override object Mnemonic extends Enum[Mnemonic] {
        val values = findValues
        case object CLREX extends Mnemonic
        case object SYS extends Mnemonic
        case object MSR extends Mnemonic
        case object HINT extends Mnemonic
        case object DSB extends Mnemonic
        case object DMB extends Mnemonic
        case object ISB extends Mnemonic
        case object SYSL extends Mnemonic
        case object MRS extends Mnemonic
      }
    }
    
    case object Unsupported extends OpClass
  }
  
  import OpClass._
  private val classOfMnem = OpClass.values.foldLeft(Map[String, OpClass]()) {
    (map, mnem) => map ++ (mnem match {
      case Unsupported =>
        List("SCVTF", "MVN", "FMOV", "FCMP", "FCVT", "FMUL", "MOVI",
             "FADD", "FSUB", "UCVTF", "FCSEL", "FDIV", "FABS", "FCCMP", "FCVTZS",
             "FNEG", "FCVTZU", "FRINTX", "FRINTP", "FRINTM", "FRINTA", "FRINTI", "EXT", "FSQRT",
             "BIF", "BIT", "DUP", "INS", "FNMUL")
      case _ => mnem.Mnemonic.values.map(_.variants).flatten
    }).map((_ -> mnem))
  }

}

case class Opcode(rawcode: String) {
  
  val mnemonic: Opcode.OpClass = {
    val lookup = rawcode.split("\\.")(0)
    if (Opcode.classOfMnem.contains(lookup))
      Opcode.classOfMnem(lookup)
    else {
      Opcode.OpClass.Unsupported
    }
  }
  
}
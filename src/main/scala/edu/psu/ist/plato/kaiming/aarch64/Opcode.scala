package edu.psu.ist.plato.kaiming.aarch64

import enumeratum._

object Opcode {
  
  sealed trait Mnemonic extends EnumEntry
  
  object Mnemonic extends Enum[Mnemonic] {
    
    val values = findValues
    
    case object ADD extends Mnemonic
    case object SUB extends Mnemonic
    case object MUL extends Mnemonic
    case object DIV extends Mnemonic
    case object ADR extends Mnemonic
    case object ASR extends Mnemonic
    case object LSL extends Mnemonic
    case object LSR extends Mnemonic
    case object ORR extends Mnemonic
    case object ORN extends Mnemonic
    case object NEG extends Mnemonic
    case object AND extends Mnemonic
    case object LDR extends Mnemonic
    case object LDP extends Mnemonic
    case object STR extends Mnemonic
    case object STP extends Mnemonic
    case object CMP extends Mnemonic
    case object CMN extends Mnemonic
    case object CSEL extends Mnemonic
    case object CSINC extends Mnemonic
    case object CINC extends Mnemonic
    case object CSET extends Mnemonic
    case object MOV extends Mnemonic
    case object MOVK extends Mnemonic
    case object B extends Mnemonic
    case object BL extends Mnemonic
    case object NOP extends Mnemonic
    case object EXT extends Mnemonic
    case object BFM extends Mnemonic
    case object TST extends Mnemonic
  }
  
  import Mnemonic._
  private val rawToMnem = Mnemonic.values.foldLeft(Map[String, Mnemonic]()) {
    (map, mnem) => map ++ (mnem match {  
      case LDR => List("LDR", "LDUR", "LDRSW", "LDURB").map((_ -> mnem))
      case LDP => List("LDP").map((_ -> mnem))
      case STP => List("STP").map((_ -> mnem))
      case STR => List("STR", "STUR", "SXTW", "STURB").map((_ -> mnem))
      case ADD => List("ADD", "ADDS", "ADC", "ADCS").map((_ -> mnem))
      case SUB => List("SUB", "SUBS").map((_ -> mnem))
      case ADR => List("ADR", "ADRP").map((_ -> mnem))
      case AND => List("AND", "ANDS").map((_ -> mnem))
      case TST => List("TST").map((_ -> mnem))
      case ASR => List("ASR").map((_ -> mnem))
      case B => List("B", "BR", "RET").map((_ -> mnem))
      case BL => List("BL", "BLR").map((_ -> mnem))
      case CMP => List("CMP").map((_ -> mnem))
      case CMN => List("CMN").map((_ -> mnem))
      case CSEL => List("CSEL").map((_ -> mnem))
      case CSINC => List("CSINC").map((_ -> mnem))
      case CINC => List("CINC").map((_ -> mnem))
      case CSET => List("CSET").map((_ -> mnem))
      case LSL => List("LSL").map((_ -> mnem))
      case LSR => List("LSR").map((_ -> mnem))
      case MOV => List("MOV").map((_ -> mnem))
      case MOVK => List("MOVK").map((_ -> mnem))
      case EXT => List("SXTW", "SXTH", "SXTB", "UXTW", "UXTH", "UXTB").map((_ -> mnem))
      case BFM => List("SBFX", "SBFM", "UBFX", "UBFM").map((_ -> mnem))
      case MUL => List("MUL", "UMUL").map((_ -> mnem))
      case DIV => List("SDIV", "UDIV").map((_ -> mnem))
      case NEG => List("NEG").map((_ -> mnem))
      case ORR => List("ORR").map((_ -> mnem))
      case ORN => List("ORN").map((_ -> mnem))
      case NOP => List("NOP").map((_ -> mnem))
    })
  }

}

case class Opcode(rawcode: String) {
  
  val mnemonic: Opcode.Mnemonic =
    Opcode.rawToMnem(rawcode.split("\\.")(0))
  
  def getCondition() = {
    val parts = rawcode.split("\\.")
    assert(parts.length <= 2)
    if (parts.length == 2) {
      assert(parts(0).equals("B"))
      Condition.withName(parts(1).toUpperCase())
    } else {
      Condition.AL
    }
  }
  
}
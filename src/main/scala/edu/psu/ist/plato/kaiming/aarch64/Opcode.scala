package edu.psu.ist.plato.kaiming.aarch64

import enumeratum._

object Opcode {
  
  sealed trait Mnemonic extends EnumEntry
  
  object Mnemonic extends Enum[Mnemonic] {
    
    val values = findValues
    
    case object ADD extends Mnemonic
    case object SUB extends Mnemonic
    case object MUL extends Mnemonic
    case object SDIV extends Mnemonic
    case object UDIV extends Mnemonic
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
    case object MOVK extends Mnemonic
    case object MOVZ extends Mnemonic
    case object MOV extends Mnemonic
    case object B extends Mnemonic
    case object CB extends Mnemonic
    case object BL extends Mnemonic
    case object NOP extends Mnemonic
    case object EXT extends Mnemonic
    case object BFM extends Mnemonic
    case object TST extends Mnemonic
  }
  
  import Mnemonic._
  private val rawToMnem = Mnemonic.values.foldLeft(Map[String, Mnemonic]()) {
    (map, mnem) => map ++ (mnem match {  
      case LDR => List("LDR", "LDUR", "LDRSW", "LDURB")
      case LDP => List("LDP")
      case STP => List("STP")
      case STR => List("STR", "STUR", "SXTW", "STURB")
      case ADD => List("ADD", "ADDS", "ADC", "ADCS")
      case SUB => List("SUB", "SUBS")
      case ADR => List("ADR", "ADRP")
      case AND => List("AND", "ANDS")
      case TST => List("TST")
      case ASR => List("ASR")
      case CB => List("CBZ", "CBNZ")
      case B => List("B", "BR", "RET")
      case BL => List("BL", "BLR")
      case CMP => List("CMP")
      case CMN => List("CMN")
      case CSEL => List("CSEL")
      case CSINC => List("CSINC")
      case CINC => List("CINC")
      case CSET => List("CSET")
      case LSL => List("LSL")
      case LSR => List("LSR")
      case MOV => List("MOV")
      case MOVK => List("MOVK")
      case MOVZ => List("MOVZ")
      case EXT => List("SXTW", "SXTH", "SXTB", "UXTW", "UXTH", "UXTB")
      case BFM => List("SBFX", "SBFM", "UBFX", "UBFM")
      case MUL => List("MUL", "UMUL")
      case SDIV => List("SDIV")
      case UDIV => List("UDIV")
      case NEG => List("NEG")
      case ORR => List("ORR")
      case ORN => List("ORN")
      case NOP => List("NOP")
    }).map((_ -> mnem))
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
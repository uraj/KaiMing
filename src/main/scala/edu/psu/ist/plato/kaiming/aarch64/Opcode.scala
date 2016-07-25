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
      case LDR => Vector("LDR", "LDUR", "LDRSW", "LDURB").map { x => (x -> mnem) }
      case LDP => Vector("LDP").map { x => (x -> mnem) }
      case STP => Vector("STP").map { x => (x -> mnem) }
      case STR => Vector("STR", "STUR", "SXTW", "STURB").map { x => (x -> mnem) }
      case ADD => Vector("ADD", "ADDS", "ADC", "ADCS").map { x => (x -> mnem) }
      case SUB => Vector("SUB", "SUBS").map { x => (x -> mnem) }
      case ADR => Vector("ADR", "ADRP").map { x => (x -> mnem) }
      case AND => Vector("AND", "ANDS").map { x => (x -> mnem) }
      case TST => Vector("TST").map { x => (x -> mnem) }
      case ASR => Vector("ASR").map { x => (x -> mnem) }
      case B => Vector("B", "BR", "RET").map { x => (x -> mnem) }
      case BL => Vector("BL", "BLR").map { x => (x -> mnem) }
      case CMP => Vector("CMP").map { x => (x -> mnem) }
      case CMN => Vector("CMN").map { x => (x -> mnem) }
      case CSEL => Vector("CSEL").map { x => (x -> mnem) }
      case CSINC => Vector("CSINC").map { x => (x -> mnem) }
      case CINC => Vector("CINC").map { x => (x -> mnem) }
      case CSET => Vector("CSET").map { x => (x -> mnem) }
      case LSL => Vector("LSL").map { x => (x -> mnem) }
      case LSR => Vector("LSR").map { x => (x -> mnem) }
      case MOV => Vector("MOV").map { x => (x -> mnem) }
      case MOVK => Vector("MOVK").map { x => (x -> mnem) }
      case EXT => Vector("SXTW", "SXTH", "SXTB", "UXTW", "UXTH", "UXTB").map { x => (x -> mnem) }
      case BFM => Vector("SBFX", "SBFM", "UBFX", "UBFM").map { x => (x -> mnem) }
      case MUL => Vector("MUL", "UMUL").map { x => (x -> mnem) }
      case DIV => Vector("SDIV", "UDIV").map { x => (x -> mnem) }
      case NEG => Vector("NEG").map { x => (x -> mnem) }
      case ORR => Vector("ORR").map { x => (x -> mnem) }
      case ORN => Vector("ORN").map { x => (x -> mnem) }
      case NOP => Vector("NOP").map { x => (x -> mnem) }
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
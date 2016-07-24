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
  private val rawToMnem = Vector(
      (LDR, Vector("LDR", "LDUR", "LDRSW", "LDURB")),
      (LDP, Vector("LDP")),
      (STP, Vector("STP")),
      (STR, Vector("STR", "STUR", "SXTW", "STURB")),
      (ADD, Vector("ADD", "ADDS", "ADC", "ADCS")),
      (SUB, Vector("SUB", "SUBS")),
      (ADR, Vector("ADR", "ADRP")),
      (AND, Vector("AND", "ANDS")),
      (TST, Vector("TST")),
      (ASR, Vector("ASR")),
      (B, Vector("B", "BR", "RET")),
      (BL, Vector("BL", "BLR")),
      (CMP, Vector("CMP")),
      (CMN, Vector("CMN")),
      (CSEL, Vector("CSEL")),
      (CSINC, Vector("CSINC")),
      (CINC, Vector("CINC")),
      (CSET, Vector("CSET")),
      (LSL, Vector("LSL")),
      (LSR, Vector("LSR")),
      (MOV, Vector("MOV")),
      (MOVK, Vector("MOVK")),
      (EXT, Vector("SXTW", "SXTH", "SXTB", "UXTW", "UXTH", "UXTB")),
      (BFM, Vector("SBFX", "SBFM", "UBFX", "UBFM")),
      (MUL, Vector("MUL", "UMUL")),
      (DIV, Vector("SDIV", "UDIV")),
      (NEG, Vector("NEG")),
      (ORR, Vector("ORR")),
      (ORN, Vector("ORN")),
      (NOP, Vector("NOP"))).foldLeft(Map[String, Mnemonic]()) {
        case (map, (mnem, vstr)) => map ++ vstr.map { x => (x -> mnem) }
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
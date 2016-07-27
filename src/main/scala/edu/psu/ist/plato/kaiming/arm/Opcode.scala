package edu.psu.ist.plato.kaiming.arm

import enumeratum._

object Opcode {
  
  sealed trait Mnemonic extends EnumEntry
  
  object Mnemonic extends Enum[Mnemonic] {
    
    val values = findValues
    
    case object ADD extends Mnemonic
    case object SUB extends Mnemonic
    case object MUL extends Mnemonic
    case object MULL extends Mnemonic
    case object DIV extends Mnemonic
    case object ASR extends Mnemonic
    case object LSL extends Mnemonic
    case object LSR extends Mnemonic
    case object RRX extends Mnemonic
    case object ORR extends Mnemonic
    case object ORN extends Mnemonic
    case object BIC extends Mnemonic
    case object EOR extends Mnemonic
    case object NOT extends Mnemonic
    case object AND extends Mnemonic
    case object LDR extends Mnemonic
    case object STR extends Mnemonic
    case object LDM extends Mnemonic
    case object STM extends Mnemonic
    case object PUSH extends Mnemonic
    case object POP extends Mnemonic
    case object CMP extends Mnemonic
    case object CMN extends Mnemonic
    case object MOV extends Mnemonic
    case object MOVT extends Mnemonic
    case object B extends Mnemonic
    case object BL extends Mnemonic
    case object NOP extends Mnemonic
    case object EXT extends Mnemonic
    case object BFX extends Mnemonic
    case object TST extends Mnemonic
    case object TEQ extends Mnemonic
    case object CLZ extends Mnemonic
    case object ADR extends Mnemonic
  }
  
  import Mnemonic._
  private val _rawToMnem = Mnemonic.values.foldLeft(Map[String, Mnemonic]()) {
    (map, mnem) => map ++ (mnem match {    
      case LDR => List("LDR", "LDRB", "LDRSB", "LDRH", "LDRSH").map((_ -> mnem))
      case STR => List("STR", "STRB", "STRSB", "STRH", "STRSH").map((_ -> mnem))
      case PUSH => List("PUSH").map((_ -> mnem))
      case POP => List("POP").map((_ -> mnem))
      case ADR => List("ADR").map((_ -> mnem))
      case ADD => List("ADD", "ADDS", "ADC", "ADCS").map((_ -> mnem))
      case SUB => List("SUB", "SUBS", "SBC", "SBCS", "RSB", "RSBS", "RSC", "RSCS").map((_ -> mnem))
      case AND => List("AND", "ANDS").map((_ -> mnem))
      case NOT => List("MVN").map((_ -> mnem))
      case TST => List("TST").map((_ -> mnem))
      case TEQ => List("TEQ").map((_ -> mnem))
      case CLZ => List("CLZ").map((_ -> mnem))
      case ASR => List("ASR").map((_ -> mnem))
      case B => List("B", "BX", "BXJ", "CBZ", "CBNZ").map((_ -> mnem))
      case BL => List("BL", "BLX").map((_ -> mnem))
      case CMP => List("CMP").map((_ -> mnem))
      case CMN => List("CMN").map((_ -> mnem))
      case LSL => List("LSL").map((_ -> mnem))
      case LSR => List("LSR").map((_ -> mnem))
      case RRX => List("RRX").map((_ -> mnem))
      case MOVT => List("MOVT").map((_ -> mnem))
      case EXT => List("SXT", "SXTA", "UXT", "UXTA").map((_ -> mnem))
      case BFX => List("SBFX", "UBFX").map((_ -> mnem))
      case MUL => List("MUL", "MLA", "MLS").map((_ -> mnem))
      case MULL => List("UMULL", "UMLAL", "SMULL", "SMLAL").map((_ -> mnem))
      case DIV => List("SDIV", "UDIV").map((_ -> mnem))
      case ORR => List("ORR").map((_ -> mnem))
      case ORN => List("ORN").map((_ -> mnem))
      case EOR => List("EOR").map((_ -> mnem))
      case BIC => List("BIC").map((_ -> mnem))
      case NOP => List("NOP").map((_ -> mnem))
      case MOV => List("MOV").map((_ -> mnem))
      case LDM => LSMultipleMode.values.map("LDM" + _.entryName).map((_ -> mnem))
      case STM => LSMultipleMode.values.map("STM" + _.entryName).map((_ -> mnem))
    })
  }
  
}

case class Opcode(rawcode: String) {
  
  // FIXME: Not all ARM instructions have the optional condition code at the end.
  val (mnemonic, condition) = {
    Opcode._rawToMnem.get(rawcode) match {
      case Some(mnem) => (mnem, Condition.AL)
      case None => { 
        val len = rawcode.length
        (Opcode._rawToMnem.get(rawcode.substring(0, len - 2)).get,
            Condition.withName(rawcode.substring(len - 2)))
      }
    }
  }
  
}
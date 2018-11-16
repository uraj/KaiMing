package io.github.uraj.kaiming.arm

import io.github.uraj.kaiming.utils.Exception

object Opcode {
  import enumeratum._
  
  sealed trait Mnemonic extends EnumEntry
  
  object Mnemonic extends Enum[Mnemonic] {
    
    val values = findValues
    
    case object ADD extends Mnemonic
    case object SUB extends Mnemonic
    case object MUL extends Mnemonic
    case object MULL extends Mnemonic
    case object SDIV extends Mnemonic
    case object UDIV extends Mnemonic
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
    case object BFC extends Mnemonic
    case object BFI extends Mnemonic
    
  }
  
  import Mnemonic._
  private val _rawToMnem = Mnemonic.values.foldLeft(Map[String, Mnemonic]()) {
    (map, mnem) => map ++ (mnem match {    
      case LDR => List("LDR", "LDRB", "LDRSB", "LDRH", "LDRSH")
      case STR => List("STR", "STRB", "STRSB", "STRH", "STRSH")
      case PUSH => List("PUSH")
      case POP => List("POP")
      case ADR => List("ADR")
      case ADD => List("ADD", "ADDS", "ADC", "ADCS")
      case SUB => List("SUB", "SUBS", "SBC", "SBCS", "RSB", "RSBS", "RSC", "RSCS")
      case AND => List("AND", "ANDS")
      case NOT => List("MVN")
      case TST => List("TST")
      case TEQ => List("TEQ")
      case CLZ => List("CLZ")
      case ASR => List("ASR")
      case B => List("B", "BX", "BXJ", "CBZ", "CBNZ")
      case BL => List("BL", "BLX")
      case CMP => List("CMP")
      case CMN => List("CMN")
      case LSL => List("LSL")
      case LSR => List("LSR")
      case RRX => List("RRX")
      case MOVT => List("MOVT")
      case EXT => List("SXT", "SXTA", "UXT", "UXTA")
      case BFX => List("SBFX", "UBFX")
      case MUL => List("MUL", "MLA", "MLS")
      case MULL => List("UMULL", "UMLAL", "SMULL", "SMLAL")
      case SDIV => List("SDIV")
      case UDIV => List("UDIV")
      case ORR => List("ORR")
      case ORN => List("ORN")
      case EOR => List("EOR")
      case BIC => List("BIC")
      case NOP => List("NOP")
      case MOV => List("MOV")
      case LDM => LSMultipleMode.values.map("LDM" + _.entryName)
      case STM => LSMultipleMode.values.map("STM" + _.entryName)
      case BFC => List("BFC")
      case BFI => List("BFI")
    }).map((_ -> mnem))
  }
  
}

case class Opcode(rawcode: String) {
  
  // FIXME: Not all ARM instructions put the optional condition code at the end.
  val (mnemonic, condition) = {
    Opcode._rawToMnem.get(rawcode) match {
      case Some(mnem) => (mnem, Condition.AL)
      case None => {
        val len = rawcode.length
        (Opcode._rawToMnem.get(rawcode.substring(0, len - 2)).get,
            Condition.withNameOption(rawcode.substring(len - 2)) match {
              case Some(c) => c
              case None => {
                val alias = Map(("CC" -> Condition.LO), ("CS" -> Condition.HS)) 
                alias.get(rawcode.substring(len - 2)) match {
                  case Some(c) => c
                  case None => Exception.unreachable("Unrecognized opcode: " + rawcode)
                }
              }
            })
      }
    }
  }
  
}

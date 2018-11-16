package io.github.uraj.kaiming.arm

import io.github.uraj.kaiming.MachFlag

import enumeratum._

sealed abstract class Condition(val dependentFlags: Set[Flag]) extends EnumEntry {
  
  def this(iv: Condition) {
    this(iv.dependentFlags)
  }
  
  final def invert = Condition.invert(this)
  
  // We need this because scala.collection.immutable.Set is invariant
  val dependentMachFlags = dependentFlags.map { f => f.asInstanceOf[MachFlag[ARM]] }

}

object Condition extends Enum[Condition] {
  
  val values = findValues
    
  case object AL extends Condition(Set[Flag]())
  case object NV extends Condition(AL)
  case object EQ extends Condition(Set[Flag](Flag.Z))
  case object NE extends Condition(EQ)
  case object HS extends Condition(Set[Flag](Flag.C))
  case object LO extends Condition(HS)
  case object MI extends Condition(Set[Flag](Flag.N))
  case object PL extends Condition(MI)
  case object VS extends Condition(Set[Flag](Flag.V))
  case object VC extends Condition(VS)
  case object HI extends Condition(Set[Flag](Flag.C, Flag.Z))
  case object LS extends Condition(HI)
  case object GE extends Condition(Set[Flag](Flag.N, Flag.V))
  case object LT extends Condition(GE)
  case object GT extends Condition(Set[Flag](Flag.N, Flag.V, Flag.Z))
  case object LE extends Condition(GT)
  
  final def invert(cond: Condition) : Condition = cond match {
    case AL => NV
    case NV => AL
    case EQ => NE
    case NE => EQ
    case GE => LT
    case LT => GE
    case GT => LE
    case LE => GT
    case HI => LS
    case LS => HI
    case HS => LO
    case LO => HS
    case MI => PL
    case PL => MI
    case VC => VS
    case VS => VC
  }

}

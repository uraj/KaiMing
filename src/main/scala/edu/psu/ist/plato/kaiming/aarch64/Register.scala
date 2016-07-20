package edu.psu.ist.plato.kaiming.aarch64

import edu.psu.ist.plato.kaiming.MachRegister
import edu.psu.ist.plato.kaiming.Arch.AArch64

import enumeratum._

object Shift {
  sealed trait Type extends EnumEntry
  object Type extends Enum[Type] {
    
    val values = findValues 
    
    case object ASR extends Type
    case object LSL extends Type
    case object ROR extends Type
    
  }
}

case class Shift(val ty: Shift.Type, val value: Int)

object Register {
  
  sealed trait Id extends EnumEntry
  
  object Id extends Enum[Id] {
    
    val values = findValues
  
    case object X0 extends Id
    case object X1 extends Id
    case object X2 extends Id
    case object X3 extends Id
    case object X4 extends Id
    case object X5 extends Id
    case object X6 extends Id
    case object X7 extends Id
    case object X8 extends Id
    case object X9 extends Id
    case object X10 extends Id
    case object X11 extends Id
    case object X12 extends Id
    case object X13 extends Id
    case object X14 extends Id
    case object X15 extends Id
    case object X16 extends Id
    case object X17 extends Id
    case object X18 extends Id
    case object X19 extends Id
    case object X20 extends Id
    case object X21 extends Id
    case object X22 extends Id
    case object X23 extends Id
    case object X24 extends Id
    case object X25 extends Id
    case object X26 extends Id
    case object X27 extends Id
    case object X28 extends Id
    case object X29 extends Id
    case object X30 extends Id
    case object X31 extends Id
    case object W0 extends Id
    case object W1 extends Id
    case object W2 extends Id
    case object W3 extends Id
    case object W4 extends Id
    case object W5 extends Id
    case object W6 extends Id
    case object W7 extends Id
    case object W8 extends Id
    case object W9 extends Id
    case object W10 extends Id
    case object W11 extends Id
    case object W12 extends Id
    case object W13 extends Id
    case object W14 extends Id
    case object W15 extends Id
    case object W16 extends Id
    case object W17 extends Id
    case object W18 extends Id
    case object W19 extends Id
    case object W20 extends Id
    case object W21 extends Id
    case object W22 extends Id
    case object W23 extends Id
    case object W24 extends Id
    case object W25 extends Id
    case object W26 extends Id
    case object W27 extends Id
    case object W28 extends Id
    case object W29 extends Id
    case object W30 extends Id
    case object W31 extends Id
    
    case object XZR extends Id
    case object WZR extends Id
    case object SP extends Id
    
  }
  
  def get(name: String, sh: Shift) = Register(Id.withName(name), Some(sh))
  def get(name: String) = Register(Id.withName(name), None)
  def get(id: Id, sh: Option[Shift]) = Register(id, sh)
  
}

case class Register(id: Register.Id, shift: Option[Shift])
  extends MachRegister[AArch64] with Operand {
  
  // implemention of abstract defs in MachRegister
  override val name = id.entryName
  override val sizeInBits = if (name == "SP" || name.startsWith("X")) 64 else 32
  override lazy val containingRegister = 
    if (name.startsWith("W"))
      Register(Register.Id.withName('X' + name.substring(1)), None)
    else
      Register(id, None)
  override val subsumedRegisters = 
    if (name.startsWith("X"))
      Set[MachRegister[AArch64]](Register.get('W' + name.substring(1)))
    else
      Set[MachRegister[AArch64]]()
  
  val isShifted = shift.isDefined
  
  override def asRegister = asInstanceOf[Register]
  
}



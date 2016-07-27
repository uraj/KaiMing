package edu.psu.ist.plato.kaiming.arm

import edu.psu.ist.plato.kaiming.MachRegister
import edu.psu.ist.plato.kaiming.Arch.ARM

import enumeratum._

sealed trait Operand {
  
  def isRegister = this.isInstanceOf[Register]
  def isMemory = this.isInstanceOf[Memory]
  def isImmediate = this.isInstanceOf[Immediate]
  
  def asImmediate: Immediate = 
    throw new UnsupportedOperationException(this + "is not an immediate operand") 

  def asMemory: Memory = 
    throw new UnsupportedOperationException(this + "is not a memory operand") 
  
  def asRegister: Register = 
    throw new UnsupportedOperationException(this + "is not a register operand") 
  
}

object Memory {
    
  def get(base: Register) = Memory(Some(base), None)
  def get(imm: Immediate) = Memory(None, Some(Left(imm)))
  def get(base: Register, imm: Immediate) = Memory(Some(base), Some(Left(imm)))
  def get(base: Register, off: Register) = Memory(Some(base), Some(Right(off)))
  
}

case class Memory(base: Option[Register], off: Option[Either[Immediate, Register]])
  extends Operand {
  
  override def asMemory = asInstanceOf[Memory]
  
}

object Immediate {
  
  def get(value: Long) = Immediate(value) 

}

case class Immediate(val value: Long) extends Operand {
  
  override def asImmediate = asInstanceOf[Immediate]
  
}


sealed abstract class Shift { val value: Int }
case class Asr(override val value: Int) extends Shift
case class Lsl(override val value: Int) extends Shift
case class Ror(override val value: Int) extends Shift

object Register {
  
  sealed trait Id extends EnumEntry
  
  object Id extends Enum[Id] {
    
    val values = findValues
  
    case object R0 extends Id
    case object R1 extends Id
    case object R2 extends Id
    case object R3 extends Id
    case object R4 extends Id
    case object R5 extends Id
    case object R6 extends Id
    case object R7 extends Id
    case object R8 extends Id
    case object R9 extends Id
    case object R10 extends Id
    case object R11 extends Id
    case object R12 extends Id
    
    case object SP extends Id
    case object LR extends Id
    case object PC extends Id
    
  }
  
  import scala.language.implicitConversions
  implicit def idToRegister(id: Id) = Register(id, None) 
  
  def get(name: String, sh: Shift) = Register(Id.withName(name), Some(sh))
  def get(name: String) = Register(Id.withName(name), None)
  
}

case class Register(id: Register.Id, shift: Option[Shift])
  extends MachRegister[ARM] with Operand {
  
  override val name = id.entryName
  override val sizeInBits = if (name == "SP" || name.startsWith("X")) 64 else 32
  override lazy val containingRegister = this 
  override val subsumedRegisters = Set[MachRegister[ARM]]()
  
  val isShifted = shift.isDefined
  
  override def asRegister = asInstanceOf[Register]
  
}


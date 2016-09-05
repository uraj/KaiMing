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
  
  def sizeInBits: Int
  
}

sealed trait OffSign
case object Positive extends OffSign
case object Negative extends OffSign

object Memory {
  
  def get(base: Register) =
    Memory(Some(base), Left(0))
    
  def get(imm: Long) = Memory(None, Left(imm))

  def get(base: Register, imm: Long) = 
    Memory(Some(base), Left(imm))
    
  def get(base: Register, off: Register, sign: OffSign = Positive) =
    Memory(Some(base), Right((sign, off)))

}

case class Memory(base: Option[Register], off: Either[Long, Tuple2[OffSign, Register]])
  extends Operand {
  
  override def asMemory = asInstanceOf[Memory]
  
  override def sizeInBits = ARMMachine.wordSizeInBits
  
}

object Immediate {
  
  def get(value: Long) = Immediate(value, ARMMachine.wordSizeInBits)

}

case class Immediate(val value: Long, override val sizeInBits: Int) extends Operand {

  override def asImmediate = asInstanceOf[Immediate]
  
  def resize(newSize: Int) = Immediate(value, newSize)
  
}


sealed abstract class Shift { val value: Int }
case class Asr(override val value: Int) extends Shift
case class Lsl(override val value: Int) extends Shift
case class Lsr(override val value: Int) extends Shift
case class Ror(override val value: Int) extends Shift
case class Rrx() extends Shift {
  override val value = throw new UnsupportedOperationException
}


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
  override val sizeInBits = 32
  override lazy val containingRegister = this 
  override val subsumedRegisters = Set[MachRegister[ARM]]()
  
  val isShifted = shift.isDefined
  
  override def asRegister = asInstanceOf[Register]
  
}



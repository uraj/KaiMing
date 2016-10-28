package edu.psu.ist.plato.kaiming.aarch64

import edu.psu.ist.plato.kaiming.MachRegister
import edu.psu.ist.plato.kaiming.Arch.AArch64

import enumeratum._

sealed trait Operand {
  
  final def isRegister = this.isInstanceOf[Register]
  final def isMemory = this.isInstanceOf[Memory]
  final def isImmediate = this.isInstanceOf[Immediate]
  final def isShiftedRegister = this.isInstanceOf[ShiftedRegister]
  final def isRegisterOrShifted = isRegister || isShiftedRegister
  
  def asImmediate: Immediate = 
    throw new UnsupportedOperationException(this + "is not an immediate operand") 

  def asMemory: Memory = 
    throw new UnsupportedOperationException(this + "is not a memory operand") 
  
  def asRegister: Register = 
    throw new UnsupportedOperationException(this + "is not a register operand")
  
  def asShiftedRegister: ShiftedRegister =
    throw new UnsupportedOperationException(this + "is not a shifted register operand")
  
  def sizeInBits: Int
}

sealed abstract class Shift { val value: Int }
case class Asr(override val value: Int) extends Shift
case class Lsl(override val value: Int) extends Shift
case class Ror(override val value: Int) extends Shift

object Register {
  
  sealed trait Id extends EnumEntry {
    val (no, prefix) =
      try {
        (entryName.substring(1).toInt, entryName.charAt(0))
      }
      catch {
        case _: NumberFormatException => (-1, '\0')
      }
  }
  
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
  
  private val _singletons = Id.values.map(x => (x -> Register(x))).toMap
  
  implicit def get(name: String) = _singletons(Id.withName(name))
  implicit def get(id: Register.Id) = _singletons(id)
  
}

case class Register private (id: Register.Id)
  extends MachRegister[AArch64] with Operand {
  
  override val name = id.entryName 
  override val sizeInBits = if (name == "SP" || name.startsWith("X")) 64 else 32
  override lazy val containingRegister = 
    if (id.prefix == 'W')
      Register.get("X" + id.no)
    else
      this
  override lazy val subsumedRegisters = 
    if (id.prefix == 'X')
      Set[MachRegister[AArch64]](Register.get("W" + id.no))
    else
      Set[MachRegister[AArch64]]()
  
  override def asRegister = this
  
}

object ShiftedRegister {
  
    def get(id: Register.Id, sh: Option[Shift]) = ShiftedRegister(Register.get(id), sh)
    def get(name: String, sh: Shift) = ShiftedRegister(Register.get(Register.Id.withName(name)), Some(sh))

}

case class ShiftedRegister(reg: Register, shift: Option[Shift]) extends Operand {
  
  def isShifted = shift.isDefined
  override def asShiftedRegister = this
  
  override def sizeInBits = reg.sizeInBits 
  
}

object Memory {
    
  def get(base: Register) = Memory(Some(base), None)
  def get(imm: Immediate) = Memory(None, Some(Left(imm.resize(AArch64Machine.wordSizeInBits))))
  def get(base: Register, imm: Immediate) = Memory(Some(base), Some(Left(imm.resize(base.sizeInBits))))
  def get(base: Register, off: ShiftedRegister) = Memory(Some(base), Some(Right(off)))
  
}

case class Memory(base: Option[Register], off: Option[Either[Immediate, ShiftedRegister]])
  extends Operand {
  
  override def asMemory = this
  override def sizeInBits = AArch64Machine.wordSizeInBits
  
}

object Immediate {
  
    def apply(value: Long) = new Immediate(value, 0)

}

// AArch64 immediate numbers do not have a unified format like A32 did.  
case class Immediate(val value: Long, override val sizeInBits: Int) extends Operand {
  
  override def asImmediate = this
  def resize(newSize: Int) = Immediate(value, newSize)
  
}


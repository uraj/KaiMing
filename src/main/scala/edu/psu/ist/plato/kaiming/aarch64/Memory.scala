package edu.psu.ist.plato.kaiming.aarch64

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
package edu.psu.ist.plato.kaiming.arm64

trait Operand {
  
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
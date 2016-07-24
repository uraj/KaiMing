package edu.psu.ist.plato.kaiming.aarch64

object Immediate {
  
  def get(value: Long) = Immediate(value) 

}

case class Immediate(val value: Long) extends Operand {
  
  override def asImmediate = asInstanceOf[Immediate]
  
}
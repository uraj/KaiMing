package edu.psu.ist.plato.kaiming.utils

final class RefWrapper[T](private val stuffing: AnyRef) {
  override final def hashCode = stuffing.hashCode
  override def equals(that: Any) = that match {
    case r: RefWrapper[_] => this.stuffing eq r.stuffing
    case _ => false
  }
}

class UnreachableCodeException(message: String) extends RuntimeException(message)
class ParsingException(message: String) extends RuntimeException(message)
class UnsupportedLanguageException(message: String) extends RuntimeException(message)

object Exception {
  
  def parseError(message: String = null): Nothing =
    throw new ParsingException(message)
  
  def unreachable(message: String = null): Nothing = 
    throw new UnreachableCodeException(message)
  
}
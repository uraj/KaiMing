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

trait ParserTrait { self: scala.util.parsing.combinator.Parsers =>

  private def escape(raw: String): String = {
    import scala.reflect.runtime.universe._
    val escaped = Literal(Constant(raw)).toString
    escaped.substring(1, escaped.size - 1)
  }
  
  protected val newline = """((\r\n)|\n|\r)+""".r
  
  protected val whitespaceWithoutNewline = 
    escape("[" + (Set('\t', ' ', '\r', '\n') &~ System.getProperty("line.separator").toSet).mkString + "]+").r
    
  protected def EOI: Parser[Any] =
    new Parser[Any] {
      def apply(in: Input) = {
        if (in.atEnd) new Success( "EOI", in )
        else Failure("End of Input expected", in)
      }
  }
  
}
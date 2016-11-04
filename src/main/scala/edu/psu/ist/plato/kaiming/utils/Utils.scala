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
  
  def unsupported(message: String = null): Nothing =
    throw new UnsupportedOperationException(message)
  
}

trait ParserTrait { self: scala.util.parsing.combinator.RegexParsers =>

  private def escape(raw: String): String = {
    import scala.reflect.runtime.universe._
    val escaped = Literal(Constant(raw)).toString
    escaped.substring(1, escaped.size - 1)
  }
  
  protected val newline = """((\r\n)|\n|\r)+""".r
  
  protected val whitespaceWithoutNewline = 
    escape("[" + (Set('\t', ' ', '\r', '\n') &~ System.getProperty("line.separator").toSet).mkString + "]+").r
    
  protected val EOI: Parser[Any] =
    new Parser[Any] {
      def apply(in: Input) = {
        if (self.skipWhitespace) {
          val offset = in.offset
          val start = self.handleWhiteSpace(in.source, offset)
          if (in.source.length == start)
            Success("EOI", in)
          else Failure("end of input expected", in)
        } else {
          if (in.atEnd) Success("EOI", in)
          else Failure("end of input expected", in)
        }
      }
    }
  
  protected def parseInteger(input: String, radix: Long): Long = {
    val lower = input.toLowerCase
    (0 until lower.length).foldLeft(0L) {
      case (sum, x) =>
        val c = lower.charAt(x)
        val digit = if (c.isDigit) c - '0' else (c - 'a') + 10
        sum * radix + digit
    }
  }
  
  protected def regexFromEnum(enum: enumeratum.Enum[_ <: enumeratum.EnumEntry]) = 
    ("(?i)(" + enum.values.map(_.entryName).sorted(Ordering[String].reverse).mkString("|")+")").r
  
}
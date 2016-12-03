package edu.psu.ist.plato.kaiming.utils

final class RefWrapper[T](private val stuffing: AnyRef) {
  override def hashCode = stuffing.hashCode
  override def equals(that: Any) = that match {
    case r: RefWrapper[_] => this.stuffing eq r.stuffing
    case _ => false
  }
}

trait Indexed extends Ordered[Indexed] {
  
  val index: Long
  final override def compare(that: Indexed) = 
    Math.signum(index - that.index).toInt
  
}

object Indexed {
  
  def binarySearch(sorted: IndexedSeq[Indexed], target: Long) = {
    def impl(left: Int, right: Int): Option[Int] = {
      if (left > right)
        None
      else {
        val mid = (left + right) / 2
        val idx = sorted(mid).index
        if (idx == target)
          Some(mid)
        else if (idx > target)
          impl(left, mid - 1)
        else
          impl(mid + 1, right)
      }
    }
    impl(0, sorted.size - 1)
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

trait FastParserTrait {
  
  protected def parseInteger(input: String, radix: Long): Long = {
    val lower = input.toLowerCase
    (0 until lower.length).foldLeft(0L) {
      case (sum, x) =>
        val c = lower.charAt(x)
        val digit = if (c.isDigit) c - '0' else (c - 'a') + 10
        sum * radix + digit
    }
  }
  
  import fastparse.noApi._
  
  protected val alpha = CharIn('a' to 'z')
  protected val ALPHA = CharIn('A' to 'Z')
  protected val Alpha = CharIn('a' to 'z', 'A' to 'z')
  protected val digit = CharIn('0' to '9')
  protected val aldigit = CharIn('a' to 'z', '0' to '9')
  protected val ALDIGIT = CharIn('A' to 'Z', '0' to '9')
  protected val Aldigit = CharIn('a' to 'z', 'A' to 'Z', '0' to '9')
  
  protected def enum(e: enumeratum.Enum[_ <: enumeratum.EnumEntry]) = 
    StringInIgnoreCase(e.values.map(_.entryName).sorted(Ordering[String].reverse):_*)
  
}
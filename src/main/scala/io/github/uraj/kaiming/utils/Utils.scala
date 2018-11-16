package io.github.uraj.kaiming.utils

trait Indexed extends Ordered[Indexed] {
  
  def index: Long
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

trait ParserTrait {
  
  protected def parseInteger(input: String, radix: Long): Long =
    input.toLowerCase.foldLeft(0L) {
      (sum, c) => sum * radix + (if (c.isDigit) c - '0' else (c - 'a') + 10)
    }
  
  val White = fastparse.WhitespaceApi.Wrapper{
    import fastparse.all._
    NoTrace(CharIn(" \t\r").rep)
  }
  
  import fastparse.noApi._
  import White._
  
  protected val alpha = CharIn('a' to 'z')
  protected val ALPHA = CharIn('A' to 'Z')
  protected val Alpha = CharIn('a' to 'z', 'A' to 'z')
  protected val digit = CharIn('0' to '9')
  protected val aldigit = CharIn('a' to 'z', '0' to '9')
  protected val ALDIGIT = CharIn('A' to 'Z', '0' to '9')
  protected val Aldigit = CharIn('a' to 'z', 'A' to 'Z', '0' to '9')
  
  protected val newline: P[Unit] = "\n".rep(1)
  
  protected val end: P[Unit] = newline | End
  
  protected val dec: P[Long] = digit.repX(1).!.map(parseInteger(_, 10))
  
  protected val hex: P[Long] =
    ("0" ~~ CharIn("xX") ~~ CharIn('0' to '9', 'a' to 'f', 'A' to 'F').repX(1).!).map(parseInteger(_, 16))
  
  protected val positive: P[Long] = hex | dec
  
  protected val integer: P[Long] = ("-".?.! ~ positive) map { 
    case ("", positive) => positive
    case (_, positive) => -positive
  }
  
  protected val plainLabel: P[String] =
    (Alpha ~~ CharIn("_-@.", 'a' to 'z', 'A' to 'Z', '0' to '9').repX).! ~~ ":"
  
  protected val quotedLabel: P[String] = "\"" ~~ (!"\"" ~~ AnyChar).repX(1).! ~~ "\":"
  
  protected val label = plainLabel | quotedLabel
  
  protected def enum(e: enumeratum.Enum[_ <: enumeratum.EnumEntry]) = 
    StringInIgnoreCase(e.values.map(_.entryName).sorted(Ordering[String].reverse):_*)
  
}
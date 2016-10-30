package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming.MachArch
import edu.psu.ist.plato.kaiming.MachRegister
import edu.psu.ist.plato.kaiming.MachFlag

object Expr {
  
  sealed abstract class Visitor[A] {
    
    protected type InfoT = A

    protected def merge(be: BExpr, o1: A, o2: A): A
    protected def lift(be: UExpr, o1: A): A

    protected sealed abstract class Action { val info: A }
    protected case class SkipChildren(override val info: A) extends Action
    protected case class VisitChildren(override val info: A) extends Action
    protected case class ReplaceWith(override val info: A, expr: Expr) extends Action
    protected case class ReplaceWithPost(override val info: A, expr: Expr,
        post: Tuple2[A, Expr] => (A, Expr)) extends Action
    
    final def visit(info: A, expr: Expr): (A, Expr) = {
      def visitChildren(i: A, expr: Expr) = expr match {
        case c: Const => doVisitConst(i, c)
        case be: BExpr => doVisitBExpr(i, be)
        case ue: UExpr => doVisitUExpr(i, ue)
        case lv: Lval => doVisitLval(i, lv)
      }
      visitExpr(info, expr) match {
        case SkipChildren(i) => (i, expr)
        case VisitChildren(i) => visitChildren(i, expr)
        case ReplaceWith(i, e) => (i, e)
        case ReplaceWithPost(i, e, f) => {
          val (ii, ee) = visit(i, e)
          f(ii, ee)
        }
      }
    }
    
    private def doVisitBExpr(info: A, be: BExpr) =
      visitBExpr(info, be) match {
        case SkipChildren(i) => (i, be)
        case VisitChildren(i) => {
          val (il, left) = visit(i, be.leftSub)
          val (ir, right) = visit(i, be.rightSub)
          if (!(left eq be.leftSub) || !(right eq be.rightSub))
            (merge(be, il, ir), be.gen(left, right))
          else
            (merge(be, il, ir), be)
        }
        case ReplaceWith(i, e) => (i, e)
        case ReplaceWithPost(i, e, f) => {
          val (ii, ee) = visit(i, e)
          f(ii, ee)
        }
      }
    
    private def doVisitUExpr(info: A, ue: UExpr): (A, Expr) =
      visitUExpr(info, ue) match {
        case SkipChildren(i) => (i, ue)
        case ReplaceWith(i, e) => (i, e)
        case VisitChildren(i) => {
          val (ii, sub) = visit(i, ue.sub)
          (lift(ue, ii), if (!(sub eq ue.sub)) ue.gen(sub) else ue)
        }
        case ReplaceWithPost(i, e, f) => f(visit(i, e))
        
      }
        
    private def doVisitConst(info: A, c: Const): (A, Expr) =
      visitConst(info, c) match {
        case ReplaceWith(i, e) => (i, e)
        case ReplaceWithPost(i, e, f) => f(visit(i, e))
        case a: Action => (a.info, c)
      }

    private def doVisitLval(info: A, lv: Lval): (A, Expr) =
      visitLval(info, lv) match {
        case ReplaceWith(i, e) => (i, e)
        case SkipChildren(i) => (i, lv)
        case VisitChildren(i) =>
          lv match {
            case r: Reg => doVisitReg(i, r)
            case v: Var => doVisitVar(i, v)
            case f: Flg => doVisitFlg(i, f)
          }
        case ReplaceWithPost(i, e, f) => f(visit(i, e))
      }

    private def doVisitVar(info: A, v: Var): (A, Expr) =
      visitVar(info, v) match {
        case ReplaceWith(i, e) => (i, e)
        case ReplaceWithPost(i, e, f) => f(visit(i, e))
        case a: Action => (a.info, v)
      }
    
    private def doVisitReg(info: A, r: Reg): (A, Expr) = 
      visitReg(info, r) match {
        case ReplaceWith(i, e) => (i, e)
        case ReplaceWithPost(i, e, f) => f(visit(i, e))
        case a: Action => (a.info, r)
      }
    
    private def doVisitFlg(info: A, flg: Flg): (A, Expr) = 
      visitFlg(info, flg) match {
        case ReplaceWith(i, e) => (i, e)
        case ReplaceWithPost(i, e, f) => f(visit(i, e))
        case a: Action => (a.info, flg)
      }
    
    protected def visitExpr(info: A, expr: Expr): Action
    protected def visitBExpr(info: A, expr: BExpr): Action
    protected def visitUExpr(info: A, expr: UExpr): Action
    protected def visitLval(info: A, lv: Lval): Action
    protected def visitVar(info: A, v: Var): Action
    protected def visitReg(info: A, r: Reg): Action
    protected def visitFlg(info: A, f: Flg): Action
    protected def visitConst(info: A, c: Const): Action
    
  }
  
  abstract class NopVisitor[A] extends Visitor[A] {
    protected def visitExpr(info: A, expr: Expr): Action = VisitChildren(info)
    protected def visitBExpr(info: A, expr: BExpr): Action = VisitChildren(info)
    protected def visitUExpr(info: A, expr: UExpr): Action = VisitChildren(info)
    protected def visitLval(info: A, lv: Lval): Action = VisitChildren(info)
    protected def visitVar(info: A, v: Var): Action = VisitChildren(info)
    protected def visitReg(info: A, r: Reg): Action = VisitChildren(info)
    protected def visitFlg(info: A, f: Flg): Action = VisitChildren(info)
    protected def visitConst(info: A, c: Const): Action = VisitChildren(info)
  }
  
  private object LvalProbe extends NopVisitor[Set[Lval]] {
    
    override protected def merge(be: BExpr, s1: Set[Lval], s2: Set[Lval]) = s1 ++ s2
    override protected def lift(ue: UExpr, s: Set[Lval]) = s
    override protected def visitLval(lvals: Set[Lval], lv: Lval) = 
      SkipChildren(lvals + lv)
      
    def probe(e: Expr) = visit(Set(), e)._1
  }
  
}

sealed abstract class Expr(sub: Expr*) {
  
  override def toString = { 
    val baos = new java.io.ByteArrayOutputStream
    val ps = new IRPrinter(baos);
    ps.printExpr(this)
    new String(baos.toByteArray)
  }
  
  val subExpr = sub.toVector
  
  def hashCode: Int
  
  def sizeInBits: Int
  
  final def substitute(o: Expr, n: Expr): Expr = 
    if (this == o) n else this match {
      case lv: Lval => if (lv == o) n else this
      case be: BExpr => {
        val left = be.leftSub.substitute(o, n)
        val right = be.rightSub.substitute(o, n)
        if (!(left eq be.leftSub) || !(right eq be.rightSub))
          be.gen(left, right)
        else
          this
      }
      case ue: UExpr => {
        val sub = ue.sub.substitute(o, n)
        if (!(sub eq ue.sub)) ue.gen(sub) else this
      }
      case _ => this
    }

  private val _substitutor = new Expr.NopVisitor[Map[Lval, Expr]] {
    
    override def merge(be: BExpr, left: InfoT, right: InfoT) = left
    override def lift(ue: UExpr, info: InfoT) = info
    
    override def visitLval(subMap: InfoT, lv: Lval) =
      subMap.get(lv) match {
        case None => SkipChildren(subMap)
        case Some(expr) => ReplaceWith(subMap, expr)
      }

  }
  
  final def substituteLvals(map: Map[Lval, Expr]): Expr =
    _substitutor.visit(map, this)._2
  
  final def contains(o: Expr) : Boolean =
    if (this == o) true else this match {
      case be: BExpr => be.leftSub.contains(o) || be.rightSub.contains(o)
      case ue: UExpr => ue.sub.contains(o)
      case _ => false
    }
  
  final def enumLvals = Expr.LvalProbe.probe(this)
  
  final def isCompound = this.isInstanceOf[CompoundExpr]
  
  final def +(right: Expr) = Add(this, right)
  final def -(right: Expr) = Sub(this, right)
  final def |(right: Expr) = Or(this, right)
  final def &(right: Expr) = And(this, right)
  final def ^(right: Expr) = Xor(this, right)
  final def *(right: Expr) = Mul(this, right)
  final def -/(right: Expr) = SDiv(this, right)
  final def +/(right: Expr) = UDiv(this, right)
  final def :+(right: Expr) = Concat(this, right)
  final def <<(right: Expr) = Shl(this, right)
  final def >>(right: Expr) = Shr(this, right)
  final def >>>(right: Expr) = Sar(this, right)
  final def ><(right: Expr) = Ror(this, right)
  final def sext(right: Const) = SExt(this, right)
  final def uext(right: Const) = UExt(this, right)
  final def |>(right: Const) = Low(this, right)
  final def |<(right: Const) = High(this, right)
  final def bswap = BSwap(this)
  final def rbit = RBit(this)
  final def unary_~ = Not(this)
  final def unary_! = Neg(this)
  final def unary_- = Const(0, this.sizeInBits) - this
  
}

sealed abstract class PrimitiveExpr extends Expr()

case class Const(value: Long, override val sizeInBits: Int) extends PrimitiveExpr {
  
  override def hashCode = value.hashCode
  
}

sealed abstract class Lval extends PrimitiveExpr {
  
  def name: String
  def sizeInBits : Int
  
}

case class Var(parent: Context, name: String, override val sizeInBits: Int)
    extends Lval {
  
  // Sometimes we want Var instances simply for testing without being binded
  // to a Context. Make hashCode null safe to parent
  override def hashCode = name.hashCode + (if (parent == null) 0 else 31 * parent.hashCode)
  
}

case class Reg(mreg: MachRegister[_ <: MachArch]) extends Lval {
  
  override def hashCode = mreg.hashCode
  override def name = mreg.name
  override def sizeInBits = mreg.sizeInBits
  
}

case class Flg(mflag: MachFlag[_ <: MachArch]) extends Lval {

  override def hashCode = mflag.hashCode
  override def name = mflag.name
  override def sizeInBits = 1

}

sealed abstract class CompoundExpr(sub: Expr*) extends Expr(sub: _*) {

  override val sizeInBits: Int

}

sealed abstract class BExpr(val leftSub: Expr, val rightSub: Expr)
  extends CompoundExpr(leftSub, rightSub) {

  override def hashCode =
    31 * (31 * leftSub.hashCode + getClass.hashCode) + rightSub.hashCode

  def gen(left: Expr, right: Expr): BExpr
  
  final def @^ = Overflow(this)
  final def @! = Carry(this)
  final def @* = Zero(this)
  final def @- = Negative(this)

}

sealed abstract class UExpr(val sub: Expr) extends CompoundExpr(sub) {
  
  override def hashCode = sub.hashCode + 31 * getClass.hashCode
  
  def gen(sub: Expr): UExpr
  
}

// Binary expressions
case class Add(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  require(leftSub.sizeInBits == rightSub.sizeInBits,
      "Operands of Add have to be of the same size")
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = Add(left, right)
  
}

case class Sub(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  require(leftSub.sizeInBits == rightSub.sizeInBits,
      "Operands of Sub have to be of the same size")
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = Sub(left, right)
  
}

case class Or(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  require(leftSub.sizeInBits == rightSub.sizeInBits,
      "Operands of Or have to be of the same size")
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = Or(left, right)
  
}

case class And(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  require(leftSub.sizeInBits == rightSub.sizeInBits,
      "Operands of And have to be of the same size")
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = And(left, right)
  
}

case class Xor(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  require(leftSub.sizeInBits == rightSub.sizeInBits,
      "Operands of Xor have to be of the same size")
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = Xor(left, right)
  
}

case class Mul(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  require(leftSub.sizeInBits == rightSub.sizeInBits,
      "Operands of Mul have to be of the same size")
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = Mul(left, right)
  
}

case class UDiv(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  require(leftSub.sizeInBits == rightSub.sizeInBits,
      "Operands of Div have to be of the same size")
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = UDiv(left, right)
  
}

case class SDiv(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  require(leftSub.sizeInBits == rightSub.sizeInBits,
      "Operands of Div have to be of the same size")
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = SDiv(left, right)
  
}

case class Concat(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {

  override val sizeInBits = leftSub.sizeInBits + rightSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = Concat(left, right)
  
}

case class Shl(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = Shl(left, right)
  
}

case class Shr(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = Shr(left, right)
  
}

case class Sar(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = Sar(left, right)
  
}

case class Ror(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override val sizeInBits = leftSub.sizeInBits
  
  override def gen(left: Expr, right: Expr) = Ror(left, right)
  
}

case class SExt(override val leftSub: Expr, override val rightSub: Const)
    extends BExpr(leftSub, rightSub) {
  
  override val sizeInBits = rightSub.value.toInt
  
  override def gen(left: Expr, right: Expr) =
    if (right.isInstanceOf[Const])
      SExt(left, right.asInstanceOf[Const])
    else
      throw new IllegalArgumentException("Right operand of SExt can only be a Const")
    
}

case class UExt(override val leftSub: Expr, override val rightSub: Const)
    extends BExpr(leftSub, rightSub) {
  
  override val sizeInBits = rightSub.value.toInt
  
  override def gen(left: Expr, right: Expr) =
    if (right.isInstanceOf[Const])
      UExt(left, right.asInstanceOf[Const])
    else
      throw new IllegalArgumentException("Right operand of UExt can only be a Const")
  
}

// extract the 0(inclusive) from rightSub (exclusive) bits from leftSub
case class Low(override val leftSub: Expr, override val rightSub: Const)
  extends BExpr(leftSub, rightSub) {
  
  require(leftSub.sizeInBits >= rightSub.value, 
      "Left operand of Low (|>) is not long enough to be truncated")
  
  override val sizeInBits = rightSub.value.toInt
  
  override def gen(left: Expr, right: Expr) = 
    if (right.isInstanceOf[Const])
      Low(left, right.asInstanceOf[Const])
    else
      throw new IllegalArgumentException("Right operand of Low can only be a Const")

}

// extract the rightSub (inclusive) to sizeof(leftSub) (exclusive) bits from leftSub
case class High(override val leftSub: Expr, override val rightSub: Const)
  extends BExpr(leftSub, rightSub) {
  
    require(leftSub.sizeInBits > rightSub.value, 
      "Left operand of High (<|) is not long enough to be truncated")
      
  override val sizeInBits = leftSub.sizeInBits - rightSub.value.toInt
  
  override def gen(left: Expr, right: Expr) =
    if (right.isInstanceOf[Const])
      High(left, right.asInstanceOf[Const])
    else
      throw new IllegalArgumentException("Right operand of High can only be a Const")
  
}
// end of binary expressions

// Unary expressions
case class Not(override val sub: Expr) extends UExpr(sub) {
  
  override val sizeInBits = sub.sizeInBits
  
  override def gen(s: Expr) = Not(s)
  
}

case class Neg(override val sub: Expr) extends UExpr(sub) {
  
  override val sizeInBits = sub.sizeInBits
  
  override def gen(s: Expr) = Neg(s)
  
}

case class BSwap(override val sub: Expr) extends UExpr(sub) {
  
  override val sizeInBits = sub.sizeInBits
  
  require(sizeInBits % 8 == 0, "Can only swap bytes")
  
  override def gen(s: Expr) = BSwap(s)
  
}

case class RBit(override val sub: Expr) extends UExpr(sub) {
  
  override val sizeInBits = sub.sizeInBits
  override def gen(s: Expr) = RBit(s)
  
}

sealed abstract class ExtractorExpr(override val sub: CompoundExpr) extends UExpr(sub) {
  
  final override val sizeInBits = 1
  
}

case class Overflow(override val sub: BExpr) extends ExtractorExpr(sub) {
  
  override def gen(s: Expr) = Overflow(s.asInstanceOf[BExpr])

}

case class Carry(override val sub: BExpr) extends ExtractorExpr(sub) {
  
  override def gen(s: Expr) = Carry(s.asInstanceOf[BExpr])

}

case class Zero(override val sub: BExpr) extends ExtractorExpr(sub) {
  
  override def gen(s: Expr) = Zero(s.asInstanceOf[BExpr])

}

case class Negative(override val sub: BExpr) extends ExtractorExpr(sub) {
  
  override def gen(s: Expr) = Negative(s.asInstanceOf[BExpr])

}
// end of unary expressions
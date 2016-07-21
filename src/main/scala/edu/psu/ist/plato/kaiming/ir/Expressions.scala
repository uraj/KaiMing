package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming.MachArch
import edu.psu.ist.plato.kaiming.MachRegister
import edu.psu.ist.plato.kaiming.MachFlag

object Expr {
  
  sealed abstract class Visitor[A] {

    protected def merge(o1: A, o2: A): A

    protected sealed abstract class Action { val info: A }
    protected case class SkipChildren(override val info: A) extends Action
    protected case class VisitChildren(override val info: A) extends Action
    protected case class ReplaceWith(override val info: A, expr: Expr) extends Action
    protected case class VisitChildrenPost(override val info: A, expr: Expr,
        func: Expr => Expr) extends Action
    
    final def visit(info: A, expr: Expr): (A, Expr) = {
      def visitChildren(i: A, expr: Expr) = expr match {
        case c: Const => doVisitConst(i, c)
        case be: BExpr => doVisitBExpr(i, be)
        case ue: UExpr => doVisitUExpr(i, ue)
        case lv: Lval => doVisitLval(i, lv)
      }
      visitExpr(info, expr) match {
        case SkipChildren(i) => (i, expr)
        case ReplaceWith(i, e) => (i, e)
        case VisitChildren(i) => visitChildren(i, expr)
        case VisitChildrenPost(i, e, f) => {
          val (ii, ee) = visit(i, e)
          (ii, f(ee))
        }
      }
    }
    
    private def doVisitBExpr(info: A, be: BExpr) =
      visitBExpr(info, be) match {
        case SkipChildren(i) => (i, be)
        case ReplaceWith(i, e) => (i, e)
        case VisitChildren(i) => {
          val (il, left) = visit(i, be.leftSub)
          val (ir, right) = visit(i, be.rightSub)
          if (!(left eq be.leftSub) || !(right eq be.rightSub))
            (merge(il, ir), be.gen(left, right))
          else
            (merge(il, ir), be)
        }
        case VisitChildrenPost(i, e, f) => {
          val (ii, ee) = visit(i, e)
          (ii, f(ee))
        }
      }
    
    private def doVisitUExpr(info: A, ue: UExpr): (A, Expr) =
      visitUExpr(info, ue) match {
        case SkipChildren(i) => (i, ue)
        case ReplaceWith(i, e) => (i, e)
        case VisitChildren(i) => {
          val (ii, sub) = visit(i, ue.sub)
          (ii, if (!(sub eq ue.sub)) ue.gen(sub) else ue)
        }
        case VisitChildrenPost(i, e, f) => {
          val (ii, ee) = visit(i, e)
          (ii, f(ee))
        }
      }
        
    private def doVisitConst(info: A, c: Const): (A, Expr) =
      visitConst(info, c) match {
        case ReplaceWith(i, e) => (i, e)
        case VisitChildrenPost(i, e, f) => {
          val (ii, ee) = visit(i, e)
          (ii, f(ee))
        }
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
        case VisitChildrenPost(i, e, f) => {
          val (ii, ee) = visit(i, e)
          (ii, f(ee))
        }
      }

    private def doVisitVar(info: A, v: Var): (A, Expr) =
      visitVar(info, v) match {
        case ReplaceWith(i, e) => (i, e)
        case VisitChildrenPost(i, e, f) => {
          val (ii, ee) = visit(i, e)
          (ii, f(ee))
        }
        case a: Action => (a.info, v)
      }
    
    private def doVisitReg(info: A, r: Reg): (A, Expr) = 
      visitReg(info, r) match {
        case ReplaceWith(i, e) => (i, e)
        case VisitChildrenPost(i, e, f) => {
          val (ii, ee) = visit(i, e)
          (ii, f(ee))
        }
        case a: Action => (a.info, r)
      }
    
    private def doVisitFlg(info: A, flg: Flg): (A, Expr) = 
      visitFlg(info, flg) match {
        case ReplaceWith(i, e) => (i, e)
        case VisitChildrenPost(i, e, f) => {
          val (ii, ee) = visit(i, e)
          (ii, f(ee))
        }
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
    
    override protected def merge(s1: Set[Lval], s2: Set[Lval]) = s1 ++ s2
    
    override protected def visitLval(lvals: Set[Lval], lv: Lval) = 
      SkipChildren(lvals + lv)
  }
  
}

sealed abstract class Expr(sub: Expr*) {
  
  val subExpr = sub.toVector
  
  def hashCode(): Int
  
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
  
  final def contains(o: Expr) : Boolean =
    if (this == o) true else this match {
      case be: BExpr => be.leftSub.contains(o) || be.rightSub.contains(o)
      case ue: UExpr => ue.sub.contains(o)
      case _ => false
    }
  
  final def enumLvals = Expr.LvalProbe.visit(Set(), this)._1
  
  final def isCompound = this.isInstanceOf[CompoundExpr]
  
  final def add(right: Expr) = Add(this, right)
  final def sub(right: Expr) = Sub(this, right)
  final def or(right: Expr) = Or(this, right)
  final def and(right: Expr) = And(this, right)
  final def xor(right: Expr) = Xor(this, right)
  final def mul(right: Expr) = Mul(this, right)
  final def div(right: Expr) = Div(this, right)
  final def concat(right: Expr) = Concat(this, right)
  final def shl(right: Expr) = Shl(this, right)
  final def shr(right: Expr) = Shr(this, right)
  final def sar(right: Expr) = Sar(this, right)
  final def ror(right: Expr) = Ror(this, right)
  final def sext(right: Expr) = SExt(this, right)
  final def uext(right: Expr) = UExt(this, right)
  final def low = Low(this)
  final def high = High(this)
  final def bswap = BSwap(this)
  final def not = Not(this)
  
}

case class Const(value: Long) extends Expr() {
  
  override def hashCode() = value.hashCode()
  
}

sealed abstract class Lval extends Expr() {
  def sizeInBits : Int
}

case class Var(parent: Context, name: String, override val sizeInBits: Int)
    extends Lval {
  
  override def hashCode() = 31 * parent.hashCode() + name.hashCode()
  
}

case class Reg(mreg: MachRegister[_ <: MachArch]) extends Lval {
  
  override def hashCode() = mreg.hashCode()
  override def sizeInBits = mreg.sizeInBits
  
}

case class Flg(mflag: MachFlag[_ <: MachArch]) extends Lval {
  
  override def hashCode() = mflag.hashCode()
  override def sizeInBits = 1
  
}

sealed abstract class CompoundExpr(sub: Expr*) extends Expr(sub: _*)

sealed abstract class BExpr(val leftSub: Expr, val rightSub: Expr) extends CompoundExpr(leftSub, rightSub) {
  
  override def hashCode() =
    31 * (31 * leftSub.hashCode() + getClass().hashCode()) + rightSub.hashCode()
    
  def gen(left: Expr, right: Expr): BExpr
  
}

sealed abstract class UExpr(val sub: Expr) extends CompoundExpr(sub) {
  
  override def hashCode() = sub.hashCode() + 31 * getClass().hashCode()
  
  def gen(sub: Expr): UExpr
  
}

// Binary expressions
case class Add(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Add(left, right)
  
}

case class Sub(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Sub(left, right)
  
}

case class Or(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Or(left, right)
  
}

case class And(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = And(left, right)
  
}

case class Xor(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Xor(left, right)
  
}

case class Mul(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Mul(left, right)
  
}

case class Div(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Div(left, right)
  
}

case class Concat(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Concat(left, right)
  
}

case class Shl(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Shl(left, right)
  
}

case class Shr(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Shr(left, right)
  
}

case class Sar(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Sar(left, right)
  
}

case class Ror(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = Ror(left, right)
  
}

case class SExt(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = SExt(left, right)
  
}

case class UExt(override val leftSub: Expr, override val rightSub: Expr)
    extends BExpr(leftSub, rightSub) {
  
  override def gen(left: Expr, right: Expr) = UExt(left, right)
  
}
// end of binary expressions

// Unary expressions
case class Not(override val sub: Expr) extends UExpr(sub) {
  
  override def gen(s: Expr) = Not(s)
  
}

case class Low(override val sub: Expr) extends UExpr(sub) {
  
  override def gen(s: Expr) = Low(s)
  
}

case class High(override val sub: Expr) extends UExpr(sub) {
  
  override def gen(s: Expr) = High(s)
  
}

case class BSwap(override val sub: Expr) extends UExpr(sub) {
  
  override def gen(s: Expr) = BSwap(s)
  
}
// end of unary expressions
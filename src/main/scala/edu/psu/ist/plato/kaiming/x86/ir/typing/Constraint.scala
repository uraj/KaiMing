package edu.psu.ist.plato.kaiming.x86.ir.typing

import scalax.collection.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._

import scala.collection.mutable.Buffer
import scala.collection.Set
import scala.collection.JavaConverters._

import edu.psu.ist.plato.kaiming.x86.ir._
import edu.psu.ist.plato.kaiming.util.UnreachableCodeException

import edu.psu.ist.plato.kaiming.elf.Elf

case class Constraint(t1 : TypeVar, t2 : TypeVar)

class ConstraintSolver(elf : Elf) {
  private var workList : List[Constraint] = Nil
  
  def add(c : Constraint) : Unit = { workList = c :: workList }
  def add(l : List[Constraint]) : Unit = l match {
    case x :: xs =>
      add(x)
      add(xs)
    case Nil =>
  }
  
  // TODO: An immediate value can be rejected as pointer at the first
  // glance
  def simpleInferConst(c : Const, id : Int) : TypeVar = 
    if (elf.withinValidRange(c.value()))
      RangedTypeVar(id)
    else
      IntVar
  
  private def getRvalTypeVarMap(irl : Buffer[Stmt]) : Map[(Stmt, Expr), TypeVar] = {
    def add(start : Int, s : Stmt, l : Set[Expr]) : List[((Stmt, Expr), TypeVar)] = 
      l.foldLeft(List[((Stmt, Expr), TypeVar)]())(
          (list, expr) => ((s, expr), 
              expr match {
                case c : Const => simpleInferConst(c, start + list.size) 
                case _ => RangedTypeVar(start + list.size)
              }
           )::list)
    irl.foldLeft(Map[(Stmt, Expr), TypeVar]())(
        (map, y) => map ++ add(map.size, y, y.enumerateRval().asScala))
  }
  
  private def getLvalTypeVarMap(start : Int, irl : Buffer[Stmt]) : Map[DefStmt, TypeVar] = {
    irl.foldLeft(Map[DefStmt, TypeVar]())(
        (map, stmt) => stmt match {
          case stmt : DefStmt => map + ((stmt, RangedTypeVar(start + map.size)))
          case _ => map
        })
  }
  
  def toConstraints(ctx : Context) : List[Constraint] = {
    val irl = ctx.entries().asScala
    val rvalMap = getRvalTypeVarMap(irl)
    val lvalMap = getLvalTypeVarMap(rvalMap.size, irl)
    val rdConstraints = rvalMap.foldLeft(List[Constraint]())(
        (list, keyvalue) => keyvalue match {
          case ((s, e), tv) => 
            if (e.isLval())
              s.searchDefFor(e.asInstanceOf[Lval]).asScala.toList.map(
                defs => lvalMap.get(defs) match {
                  case Some(tv2) => Constraint(tv, tv2)
                  case None => throw new UnreachableCodeException
                })
            else
              list
        })
    rdConstraints    
  }
  
  private def exprToConstraint(s : Stmt, e : Expr,
      rvalMap : Map[(Stmt, Expr), TypeVar]) : List[Constraint] = {
    class ConstraintGen extends Expr.Visitor {
      var ret = List[Constraint]()

      override protected def visitUExpr(e : UExpr) : Boolean = {
        val tau = rvalMap.get((s, e)).orNull
        val tau1 = rvalMap.get((s, e.subExpr())).orNull
        ret =
          Constraint(tau, ConstTypeVar(TInt))::
          Constraint(tau1, ConstTypeVar(TInt))::ret
        true
      }
      
      override protected def visitBExpr(e : BExpr) : Boolean = {
        val tau = rvalMap.get((s, e)).orNull
        val tau1 = rvalMap.get((s, e.leftSubExpr())).orNull
        val tau2 = rvalMap.get((s, e.rightSubExpr())).orNull
        e.operator() match {
          case BExpr.Op.CONCAT =>
            ret = 
              Constraint(tau, ConstTypeVar(TInt))::
              Constraint(tau1, ConstTypeVar(TInt))::
              Constraint(tau2, ConstTypeVar(TInt))::ret
          case BExpr.Op.DIV =>
            ret = 
              Constraint(tau, ConstTypeVar(TInt))::
              Constraint(tau1, ConstTypeVar(TInt))::
              Constraint(tau2, ConstTypeVar(TInt))::ret
          case BExpr.Op.MUL =>
            ret = 
              Constraint(tau, ConstTypeVar(TInt))::
              Constraint(tau1, ConstTypeVar(TInt))::
              Constraint(tau2, ConstTypeVar(TInt))::ret
          case BExpr.Op.SAR =>
            ret = 
              Constraint(tau, ConstTypeVar(TInt))::
              Constraint(tau1, ConstTypeVar(TInt))::
              Constraint(tau2, ConstTypeVar(TInt))::ret
          case BExpr.Op.SHL | BExpr.Op.SHR =>
            ret = 
              Constraint(tau, ConstTypeVar(TTop))::
              Constraint(tau1, ConstTypeVar(TTop))::
              Constraint(tau2, ConstTypeVar(TInt))::ret
          case BExpr.Op.ADD =>
          case BExpr.Op.SUB =>
          case BExpr.Op.UADD =>
          case BExpr.Op.UMUL =>
          case BExpr.Op.USUB =>
          case BExpr.Op.XOR | BExpr.Op.OR | BExpr.Op.AND =>
            // Intentionally left blank
        }
        true
      }
      
      override protected def visitConst(c : Const) : Boolean = {
        true
      }
    }
    val gen = new ConstraintGen
    gen.visit(e)
    gen.ret
  }
  
  private def constraintsToGraph(l : List[Constraint]) = {
     Graph() ++ workList.map({ case Constraint(tv, ty) => Set(ty) ~> Set(tv) })
  }
  
  // TODO: This algorithm potentially has performance issue. An optimal solution should be
  // Tarjan's strongly connected component detection algorithm
  private def solveImpl(g : Graph[Set[TypeVar], DiEdge]) : Graph[Set[TypeVar], DiEdge] = {
    g.findCycle match {
      case None => g
      case Some(cycle) => {
        val toCoalesce = cycle.nodes.map(_.value).toSet
        val preds = cycle.nodes.map(_<~|).reduce(_|_).map(_.value) -- toCoalesce
        val succs = cycle.nodes.map(_~>|).reduce(_|_).map(_.value) -- toCoalesce
        val coalesced = toCoalesce.flatten
        solveImpl(g -- cycle.nodes + coalesced ++ preds.map(_~>coalesced) ++ succs.map(coalesced~>_))
      }
    }
  }
  
  def solve() = solveImpl(constraintsToGraph(workList))
}

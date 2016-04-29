package edu.psu.ist.plato.kaiming.x86.ir.typing

import scalax.collection.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._

import scala.collection.mutable.Buffer
import scala.collection.Set
import scala.collection.JavaConverters._

import edu.psu.ist.plato.kaiming.x86.ir._
import edu.psu.ist.plato.kaiming.util.UnreachableCodeException

import edu.psu.ist.plato.kaiming.elf.Elf

sealed abstract class Constraint

sealed abstract class GraphicConstraint extends Constraint
case class Subtype(t1 : TypeVar, t2 : TypeVar) extends GraphicConstraint {
  require(t1 != null)
  require(t2 != null)
}
case class Eqtype(t1 : TypeVar, t2 : TypeVar) extends GraphicConstraint {
  require(t1 != null)
  require(t2 != null)
}

sealed abstract class NonGraphicConstraint extends Constraint
case class Add(t : TypeVar, t1: TypeVar, t2 : TypeVar) extends NonGraphicConstraint {
  require(t != null)
  require(t1 != null)
  require(t2 != null)
}
case class Sub(t : TypeVar, t1: TypeVar, t2 : TypeVar) extends NonGraphicConstraint {
  require(t != null)
  require(t1 != null)
  require(t2 != null)
}

class ConstraintSolver(elf : Elf) {
  
  private val IntVar = new ConstTypeVar(-1, TInt)
  private val PtrVar = new ConstTypeVar(-2, TPtr)
  private val TopVar = new ConstTypeVar(-3, TTop)
  private val BotVar = new ConstTypeVar(-4, TBot)

  def simpleInferConst(c : Const, id : Int) : TypeVar = 
    if (elf.withinValidRange(c.value()))
      new MutableTypeVar(id)
    else {
      println(c.value().toHexString) 
      IntVar
    }
  
  private def getRvalTypeVarMap(start : Int, irl : Buffer[Stmt]) : Map[(Stmt, Expr), TypeVar] = {
    def add(start : Int, s : Stmt, l : Set[Expr]) : List[((Stmt, Expr), TypeVar)] = 
      l.foldLeft(List[((Stmt, Expr), TypeVar)]())(
          (list, expr) => ((s, expr), 
              expr match {
                case c : Const => simpleInferConst(c, start + list.size)
                case _ => new MutableTypeVar(start + list.size)
              }
           )::list)
    irl.foldLeft(Map[(Stmt, Expr), TypeVar]())(
        (map, y) => map ++ add(start + map.size, y, y.enumerateRval().asScala))
  }
  
  private def getLvalTypeVarMap(start : Int, irl : Buffer[Stmt]) : Map[DefStmt, TypeVar] = {
    irl.foldLeft(Map[DefStmt, TypeVar]())(
        (map, stmt) => stmt match {
          case stmt : DefStmt => map + ((stmt, new MutableTypeVar(start + map.size)))
          case _ => map
        })
  }
  
  def toConstraints(ctx : Context) : (Result, List[Constraint]) = {
    val irl = ctx.entries().asScala
    val rvalMap = getRvalTypeVarMap(0, irl)
    val lvalMap = getLvalTypeVarMap(rvalMap.size, irl)
    val rdConstraints = rvalMap.foldLeft(List[Constraint]())(
        (list, keyvalue) => keyvalue match {
          case ((s, e : Lval), tv) => 
              s.searchDefFor(e).asScala.toList.map({
                defs => if (defs.isExternal()) None 
                  else Some(Subtype(lvalMap.get(defs).orNull, tv))
              }).flatten
          case _ => list
        })
    val stmtConstraints = irl.map({ 
      case s : AssignStmt =>
        Subtype(lvalMap.get(s).orNull, rvalMap.get((s, s.usedRval())).orNull)::
        exprToConstraints(s, s.usedRval(), rvalMap)
      case s : CmpStmt =>
        Nil
      case s : CallStmt =>
        Eqtype(rvalMap.get((s, s.target())).orNull, PtrVar)::
        exprToConstraints(s, s.target(), rvalMap)
      case s : JmpStmt => {
        Eqtype(rvalMap.get((s, s.target())).orNull, PtrVar)::
        exprToConstraints(s, s.target(), rvalMap)
      }
      case s : LdStmt =>
        Eqtype(rvalMap.get((s, s.loadFrom())).orNull, PtrVar)::
        exprToConstraints(s, s.loadFrom(), rvalMap)
      case s : StStmt =>
        Eqtype(rvalMap.get((s, s.storeTo())).orNull, PtrVar)::
        exprToConstraints(s, s.storeTo(), rvalMap)
      case s : RetStmt =>
        Nil
      case s : SetFlagStmt =>
        Nil
    }).flatten
    (new Result(rvalMap, lvalMap), rdConstraints ++ stmtConstraints)
  }
  
  private def exprToConstraints(s : Stmt, e : Expr,
      rvalMap : Map[(Stmt, Expr), TypeVar]) : List[Constraint] = {
    class ConstraintGen extends Expr.Visitor {
      var ret = List[Constraint]()

      override protected def visitUExpr(e : UExpr) : Boolean = {
        val tau = rvalMap.get((s, e)).orNull
        val tau1 = rvalMap.get((s, e.subExpr())).orNull
        ret =
          Subtype(tau, IntVar)::Subtype(tau1, IntVar)::ret
        true
      }
      
      override protected def visitBExpr(e : BExpr) : Boolean = {
        val tau = rvalMap.get((s, e)).orNull
        val tau1 = rvalMap.get((s, e.leftSubExpr())).orNull
        val tau2 = rvalMap.get((s, e.rightSubExpr())).orNull
        e.operator() match {
          case BExpr.Op.CONCAT | BExpr.Op.MUL | BExpr.Op.DIV | BExpr.Op.SAR =>
            ret = 
              Subtype(tau, IntVar)::
              Subtype(tau1, IntVar)::
              Subtype(tau2, IntVar)::ret
          case BExpr.Op.SHL | BExpr.Op.SHR =>
            ret = 
              Subtype(tau2, IntVar)::ret
          case BExpr.Op.ADD =>
            ret = Add(tau, tau1, tau2)::ret
          case BExpr.Op.SUB =>
            ret = Sub(tau, tau1, tau2)::ret
          case BExpr.Op.XOR | BExpr.Op.OR | BExpr.Op.AND =>
            // Intentionally left blank
        }
        true
      }
    }
    val gen = new ConstraintGen
    gen.visit(e)
    gen.ret
  }
  
  private def constraintsToGraph(l : List[GraphicConstraint]) = {
    Graph() ++ l.foldLeft(List[DiEdge[Set[TypeVar]]]())({
      (l, c) => c match { 
        case Subtype(tv, ty) => (Set(ty) ~> Set(tv))::l
        case Eqtype(tv, ty) => (Set(ty) ~> Set(tv))::(Set(ty) ~> Set(tv))::l
      }
    })
  }
  
  // TODO: This algorithm may have a performance issue. An optimal solution should be
  // Tarjan's strongly connected component detection algorithm
  private def solveImpl(g : Graph[Set[TypeVar], DiEdge]) : Graph[Set[TypeVar], DiEdge] = {
    g.findCycle match {
      case None => refineConstraints(g)
      case Some(cycle) => {
        val toCoalesce = cycle.nodes.map(_.value).toSet
        val preds = cycle.nodes.map(_<~|).reduce(_|_).map(_.value) -- toCoalesce
        val succs = cycle.nodes.map(_~>|).reduce(_|_).map(_.value) -- toCoalesce
        val coalesced = toCoalesce.flatten
        solveImpl(g -- cycle.nodes + coalesced ++ preds.map(_~>coalesced) ++ succs.map(coalesced~>_))
      }
    }
  }
  
  private def joinOfTypeVarSet(s : Set[TypeVar], useUpper : Boolean) =
    if (useUpper)
      s.foldLeft(TBot : Type)((last, v) => last \/ v.upper)
    else
      s.foldLeft(TBot : Type)((last, v) => last \/ v.lower)
      
  private def meetOfTypeVarSet(s : Set[TypeVar], useUpper : Boolean) =
    if (useUpper)
      s.foldLeft(TTop : Type)((last, v) => last /\ v.upper)
    else
      s.foldLeft(TTop : Type)((last, v) => last /\ v.lower)
  
  private def refineConstraints(g : Graph[Set[TypeVar], DiEdge]) = {
    g.nodes.map(_.value).foreach {
      inner => {
        val upper = meetOfTypeVarSet(inner.map(_.value), true)
        val lower = joinOfTypeVarSet(inner.map(_.value), false)
        inner.foreach(_.setUpperLower(upper, lower))
      }
    }
    val torder = (g.topologicalSort match {
      case Right(order) => order
      case Left(_) => throw new UnreachableCodeException
    }).toBuffer
    torder.iterator.foreach {
      inner => {
        if (!inner.head.isDetermined) {
          val lower = meetOfTypeVarSet(inner.diPredecessors.map(_.head.value), true)
          inner.foreach(_.setUpper(lower))
        }
      }
    }
    torder.reverseIterator.foreach {
      inner => {
        if (!inner.head.isDetermined) {
          val upper = joinOfTypeVarSet(inner.diSuccessors.map(_.head.value), false)
          inner.foreach(_.setLower(upper))
        }
      }
    }
    g
  }
  
  def solveGraphicConstraints(workList : List[GraphicConstraint]) = {
    solveImpl(constraintsToGraph(workList))
  }
    
  class Result(rvalMap : Map[(Stmt, Expr), TypeVar], lvalMap : Map[DefStmt, TypeVar]) {
    def queryRvalType(s : Stmt, e : Expr) : Option[TypeVar] = rvalMap.get(s, e) 
    def queryLvalType(s : DefStmt) : Option[TypeVar] = lvalMap.get(s)
    override def toString() = {
      val builder = new StringBuilder
      builder ++= "Lvalue types:\n"
      lvalMap.iterator.toBuffer.sortWith(_._1.index() < _._1.index()).foreach {
        case (s, v) => builder ++= s.toString ++= " : " ++= v.toString += '\n'
      }
      builder ++= "Rvalue types:\n"
      rvalMap.iterator.toBuffer.sortWith(_._1._1.index() < _._1._1.index()).foreach {
        case ((s, e), v) =>
          builder ++= s.toString() += ' ' ++= e.toString() ++= " : " ++= v.toString() += '\n' 
      }
      builder.toString
    }
  }
}

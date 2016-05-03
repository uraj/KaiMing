package edu.psu.ist.plato.kaiming.x86.ir.typing

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._

import scala.collection.mutable.Buffer
import scala.collection.Set
import scala.collection.JavaConverters._

import edu.psu.ist.plato.kaiming.x86.ir._
import edu.psu.ist.plato.kaiming.util.UnreachableCodeException

import edu.psu.ist.plato.kaiming.elf.Elf

sealed abstract class Constraint

sealed abstract class GraphicConstraint extends Constraint
case class Subtype(tv1 : TypeVar, tv2 : TypeVar) extends GraphicConstraint {
  require(tv1 != null)
  require(tv2 != null)
}
case class Eqtype(tv1 : TypeVar, tv2 : TypeVar) extends GraphicConstraint {
  require(tv1 != null)
  require(tv2 != null)
}

sealed abstract class NonGraphicConstraint extends Constraint
case class Add(tv : TypeVar, tv1: TypeVar, tv2 : TypeVar) extends NonGraphicConstraint {
  require(tv != null)
  require(tv1 != null)
  require(tv2 != null)
}
case class Sub(tv : TypeVar, tv1: TypeVar, tv2 : TypeVar) extends NonGraphicConstraint {
  require(tv != null)
  require(tv1 != null)
  require(tv2 != null)
}

class TypeInferer(elf : Elf) {
  
  private val IntVar = new ConstTypeVar(-1, TInt)
  private val PtrVar = new ConstTypeVar(-2, TPtr)
  private val TopVar = new ConstTypeVar(-3, TTop)
  private val BotVar = new ConstTypeVar(-4, TBot)

  def simpleInferConst(c : Const, id : Int) : TypeVar = 
    if (elf.withinValidRange(c.value()))
      new MutableTypeVar(id)
    else
      IntVar

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
  
  private def toConstraints(ctx : Context) : (Result, List[Constraint]) = {
    val irl = ctx.entries().asScala
    val rvalMap = getRvalTypeVarMap(0, irl)
    val lvalMap = getLvalTypeVarMap(rvalMap.size, irl)
    val rdConstraints = rvalMap.foldLeft(List[Constraint]())(
        (list, keyvalue) => keyvalue match {
          case ((s, e : Lval), tv) => 
              s.searchDefFor(e).asScala.toList.foldLeft(list) {
                (l, defs) =>
                  if (defs.isExternal()) l else Subtype(lvalMap.get(defs).orNull, tv)::l
              }
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
  
  private def addConstraints(g : Graph[Set[TypeVar], DiEdge],
      l : List[GraphicConstraint]) = {
    g ++ l.foldLeft(List[DiEdge[Set[TypeVar]]]())({
      (l, c) => c match { 
        case Subtype(tv, ty) => (Set(ty) ~> Set(tv))::l
        case Eqtype(tv, ty) => (Set(ty) ~> Set(tv))::(Set(ty) ~> Set(tv))::l
      }
    })
  }
  
  // TODO: This algorithm may have a performance issue. An optimal solution should be
  // Tarjan's strongly connected component detection algorithm
  private def eliminateCycles(g : Graph[Set[TypeVar], DiEdge]) : Graph[Set[TypeVar], DiEdge] = {
    g.findCycle match {
      case None => g
      case Some(cycle) => {
        val toCoalesce = cycle.nodes.map(_.value).toSet
        val preds = cycle.nodes.map(_<~|).reduce(_|_).map(_.value) -- toCoalesce
        val succs = cycle.nodes.map(_~>|).reduce(_|_).map(_.value) -- toCoalesce
        val coalesced = toCoalesce.flatten
        eliminateCycles(g -- cycle.nodes + coalesced ++ preds.map(_~>coalesced) ++ succs.map(coalesced~>_))
      }
    }
  }
  
  private def joinOfTypes(s : Set[Type]) = s.foldLeft(TBot : Type)(_\/_)
  private def meetOfTypes(s : Set[Type]) = s.foldLeft(TTop : Type)(_/\_)
  
  private def populateConstraints(g : Graph[Set[TypeVar], DiEdge]) = {
    g.nodes.map(_.value).foreach {
      inner => {
        val upper = meetOfTypes(inner.map(_.value.upper))
        val lower = joinOfTypes(inner.map(_.value.lower))
        inner.foreach(_.setUpperLower(upper, lower))
      }
    }
    val torder = (g.topologicalSort match {
      case Right(order) => order
      case Left(_) => throw new UnreachableCodeException
    }).toBuffer
    val populateUpperDirty = torder.iterator.foldLeft(false) {
      (b, inner) => {
        val lower = meetOfTypes(inner.diPredecessors.map(_.head.value.upper))
        inner.foldLeft(false) {
          (dirty, tv) => dirty || tv.setUpper(lower /\ tv.upper) 
        }
      }
    }
    val populateLowerDirty = torder.reverseIterator.foldLeft(false) {
      (b, inner) => {
        val upper = joinOfTypes(inner.diSuccessors.map(_.head.value.lower))
        inner.foldLeft(false) {
          (dirty, tv) => (dirty || tv.setLower(upper \/ tv.lower))
        }
      }
    }
    populateUpperDirty || populateLowerDirty
  }
  
  private def refineConstraints(constraints : List[NonGraphicConstraint]) : List[GraphicConstraint] = {
    constraints.foldLeft(List[GraphicConstraint]()) {
      (l, c) => c match {
        case Add(tv, tv1, tv2) => {
          tv.upper match {
            case TInt => Subtype(tv1, IntVar)::Subtype(tv2, IntVar)::l 
            case TPtr =>
              (tv1.upper, tv2.upper) match {
                case (TInt, _) => Subtype(tv2, tv)::Subtype(tv2, PtrVar)::l
                case (_, TInt) => Subtype(tv1, tv)::Subtype(tv1, PtrVar)::l
                case (TPtr, _) => Subtype(tv2, IntVar)::Subtype(tv1, tv)::l
                case (_, TPtr) => Subtype(tv1, IntVar)::Subtype(tv2, tv)::l
                case _ => l
              }
            case _ => (tv1.upper, tv2.upper) match {
              case (TInt, TInt) => Subtype(tv, IntVar)::l 
              case _ => l
            }
          } 
        }
        case Sub(tv, tv1, tv2) => {
          tv.upper match {
            case TInt => (tv1.upper, tv2.upper) match {
              case (TPtr, TPtr) => l
              case (TPtr, _) => Subtype(tv2, PtrVar)::l
              case (_, TPtr) => Subtype(tv1, PtrVar)::l
              case (_, _) => Subtype(tv1, IntVar)::Subtype(tv2, IntVar)::l
            }
            case TPtr => Subtype(tv1, tv)::Subtype(tv2, IntVar)::l
            case _ => (tv1.upper, tv2.upper) match {
              case (TInt, _) => Subtype(tv, IntVar)::Subtype(tv2, IntVar)::l
              case (_, TPtr) => Subtype(tv, IntVar)::Subtype(tv1, PtrVar)::l
              case _ => l
            }
          }
        }
      }
    }
  }
  
  private def solveConstraints(g : Graph[Set[TypeVar], DiEdge], 
      nongraphic : List[NonGraphicConstraint]) : Unit = {
    val dag = eliminateCycles(addConstraints(g, refineConstraints(nongraphic)))
    if (populateConstraints(dag)) {
      solveConstraints(dag, nongraphic)
    }
    else
      ()
  }
  
  def infer(ctx : Context) : Result = {
    val (result, constraints) = toConstraints(ctx)
    val (graphic, nonGraphic) = constraints.foldLeft(
      (List[GraphicConstraint](), List[NonGraphicConstraint]()))({
      case ((l1, l2), x : GraphicConstraint) => (x::l1, l2)
      case ((l1, l2), x : NonGraphicConstraint) => (l1, x::l2)
    })
    val g = addConstraints(Graph(), graphic)
    solveConstraints(g, nonGraphic)
    result
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

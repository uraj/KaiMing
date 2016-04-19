package edu.psu.ist.plato.kaiming.x86.ir.typing

import scalax.collection.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._

import scala.collection.GenTraversableOnce
import scala.collection.Set

sealed abstract class Constraint
  
case class Eq(left : TypeVar, right : TypeVar) extends Constraint
case class Sub(left : TypeVar, right : TypeVar) extends Constraint
case class Super(left : TypeVar, right : TypeVar) extends Constraint

class ConstraintSolver {
  private var workList : List[Constraint] = Nil
  
  def add(c : Constraint) : Unit = { workList = c::workList }
  
  private def constraintsToGraph(l : List[Constraint]) = {
     def processEachConstraint (c : Constraint) : List[DiEdge[Set[TypeVar]]] = c match {
        case Eq(tv, ty) => List(Set(tv) ~> Set(ty), Set(ty) ~> Set(tv))
        case Sub(tv, ty) => List(Set(ty) ~> Set(tv))
        case Super(tv, ty) => List(Set(tv) ~> Set(ty))
      }
     Graph() ++ workList.map(processEachConstraint).flatten
  }
  
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

package edu.psu.ist.plato.kaiming

import scala.collection.immutable.SortedSet
import scala.collection.immutable.TreeSet

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

import edu.psu.ist.plato.kaiming.Arch.KaiMing

abstract class Cfg[A <: Arch, B <: BBlock[A]] extends Iterable[B] {
  
  val parent : Procedure[A]
  val entryBlock: BBlock[A]
  val hasIndirectJmp: Boolean
  val hasDanglingJump: Boolean
  protected val graph: Graph[B, LDiEdge]
  
  lazy val blocks = graph.nodes.map(_.value).toVector.sorted[BBlock[A]]
  def entries = blocks.flatMap(_.entries)
  def iterator = blocks.iterator
  
  import scala.language.postfixOps
  
  def predecessors(bb: B): Set[B] = 
    (graph.get(bb) <~) map { _.from.value }
  def successors(bb: B): Set[B] = 
    (graph.get(bb) ~>) map { _.to.value }
  
  def isConnected = graph.isConnected
  def belongingComponent(b: B) =
    if (graph.contains(b)) {
      val inner = graph.get(b)
      graph.componentTraverser().find { _.nodes.contains(inner) }
    } else None 

  object LEdgeImplicit extends scalax.collection.edge.LBase.LEdgeImplicits[Boolean]
  import LEdgeImplicit._
  def labeledPredecessors(bb: B): Set[(B, Boolean)] = 
    (graph.get(bb) <~) map { x => (x.from.value, x: Boolean) }
  def labeledSuccessors(bb: B): Set[(B, Boolean)] = 
    (graph.get(bb) ~>) map { x => (x.to.value, x: Boolean) }

}

class MachCFG[A <: MachArch] (val parent: MachProcedure[A], val entryBlock: MachBBlock[A],
    protected val graph: Graph[MachBBlock[A], LDiEdge], val hasIndirectJmp: Boolean,
    val hasDanglingJump: Boolean) extends Cfg[A, MachBBlock[A]] {
  
  override lazy val blocks = graph.nodes.map(_.value).toVector.sorted[BBlock[A]]
  
}

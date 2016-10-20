package edu.psu.ist.plato.kaiming

import scala.collection.immutable.SortedSet
import scala.collection.immutable.TreeSet

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

import edu.psu.ist.plato.kaiming.Arch.KaiMing

abstract class Cfg[A <: Arch, B <: BBlock[A]] (private val _graph: Graph[B, LDiEdge])
    extends Iterable[B] {
  
  val parent : Procedure[A]
  val entryBlock: BBlock[A]
  val hasIndirectJmp: Boolean
  val hasDanglingJump: Boolean
  
  lazy val blocks = _graph.nodes.map(_.value).toVector.sorted[BBlock[A]]
  def entries = blocks.flatMap(_.entries)
  def iterator = blocks.iterator
  
  import scala.language.postfixOps
  
  def predecessors(bb: B): Set[B] = 
    (_graph.get(bb) <~) map { _.from.value }
  def successors(bb: B): Set[B] = 
    (_graph.get(bb) ~>) map { _.to.value }

  object LEdgeImplicit extends scalax.collection.edge.LBase.LEdgeImplicits[Boolean]
  import LEdgeImplicit._
  def labeledPredecessors(bb: B): Set[(B, Boolean)] = 
    (_graph.get(bb) <~) map { x => (x.from.value, x: Boolean) }
  def labeledSuccessors(bb: B): Set[(B, Boolean)] = 
    (_graph.get(bb) ~>) map { x => (x.to.value, x: Boolean) }

}

class MachCFG[A <: MachArch] (override val parent: MachProcedure[A],
    override val entryBlock: MachBBlock[A], graph: Graph[MachBBlock[A], LDiEdge],
    override val hasIndirectJmp: Boolean, override val hasDanglingJump: Boolean)
    extends Cfg[A, MachBBlock[A]](graph) {
  
  private val _graph: Graph[MachBBlock[A], LDiEdge] = graph
  
  override lazy val blocks = _graph.nodes.map(_.value).toVector.sorted[BBlock[A]]
  
}

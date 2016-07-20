package edu.psu.ist.plato.kaiming

import scala.collection.immutable.SortedSet
import scala.collection.immutable.TreeSet

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

class CFG[A <: Arch] (val parent : Procedure[A], val entryBlock: BasicBlock[A],
    private val _graph: Graph[BasicBlock[A], LDiEdge], val hasIndirectJump: Boolean)
    extends Iterable[BasicBlock[A]] {
  
  val blocks: SortedSet[BasicBlock[A]] =
    TreeSet[BasicBlock[A]]() ++ _graph.nodes.map(_.value)
  def entries = 
    blocks.foldLeft(List[BasicBlock[A]]()){ (a, b) => b::a }.flatMap(_.entries)
  def iterator = blocks.iterator
  
  def predecessors(bb : BasicBlock[A]): Set[BasicBlock[A]] = 
    (_graph.get(bb) <~|) map {_.value}
  def successors(bb : BasicBlock[A]): Set[BasicBlock[A]] = 
    (_graph.get(bb) ~>|) map {_.value}

}

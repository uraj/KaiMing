package edu.psu.ist.plato.kaiming

import scala.collection.immutable.SortedSet
import scala.collection.immutable.TreeSet

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

class CFG[A <: Arch] (val parent : Procedure[A], val entryBlock: BBlock[A],
    private val _graph: Graph[BBlock[A], LDiEdge], val hasIndirectJump: Boolean)
    extends Iterable[BBlock[A]] {
  
  val blocks: SortedSet[BBlock[A]] =
    TreeSet[BBlock[A]]() ++ _graph.nodes.map(_.value)
  def entries = 
    blocks.foldLeft(List[BBlock[A]]()){ (a, b) => b::a }.flatMap(_.entries)
  def iterator = blocks.iterator
  
  def predecessors(bb : BBlock[A]): Set[BBlock[A]] = 
    (_graph.get(bb) <~|) map {_.value}
  def successors(bb : BBlock[A]): Set[BBlock[A]] = 
    (_graph.get(bb) ~>|) map {_.value}

}

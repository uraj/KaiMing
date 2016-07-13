package edu.psu.ist.plato.kaiming

import scala.collection.immutable.SortedSet
import scala.collection.immutable.TreeSet

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge
import scalax.collection.edge.Implicits._

object CFG {
  private def split[T <: Entry](unit : Procedure[T], entries : Seq[T], pivots : Seq[Int]) = {
    val sortedPivots = (TreeSet[Int]() ++ pivots + 0 + entries.length).toVector
    require(sortedPivots.head == 0 && sortedPivots.last <= entries.length)
    (0 until (sortedPivots.length - 1)).foldRight(List[BasicBlock[T]]()) { 
      (i, blist) => {
        val index = entries(sortedPivots(i)).index
        BasicBlock(unit, entries.slice(sortedPivots(i), sortedPivots(i + 1)),
            unit.deriveLabelForIndex(index)) :: blist
      }
    }
  }
  
  private def containingBlock[T <: Entry](bbs : Traversable[BasicBlock[T]], index : Long) =
    bbs.find { bb => bb.lastEntry.index >= index && bb.firstEntry.index <= index }
  
  private def buildCFG[T <: Entry](parent : Procedure[T], entries : Seq[T]) = {
    val sorted = entries.toVector.sorted[Entry]
    val pivots = (0 until sorted.length).foldLeft(Vector[Int]()) {
      (vec, i) =>
        if (sorted(i).isTerminator) {
          val term = sorted(i).asInstanceOf[Entry.Terminator[T]]
          val nvec = if (!term.isCall) vec :+ (i + 1) else vec
          if (term.isIntraprocedural && !term.isIndirect && term.isTargetConcrete)
            nvec :+ Entry.search(sorted, term.targetIndex)
          else
            nvec
        } else {
          vec
        }
    }
    val bbs = split(parent, entries, pivots)
    val entry = bbs.head
    val hasIndirectJump = bbs.foldLeft(false) {
      (has, bb) => has || (bb.lastEntry.isTerminator && bb.lastEntry.asTerminator[T].isIndirect)
    }
    val edges = bbs.foldLeft(List[LDiEdge[BasicBlock[T]]]()) {
      (l, bb) => {
        val in = bb.lastEntry
        if (in.isTerminator) {
          val term = in.asTerminator[T]
          if (!term.isCall && !term.isIndirect && term.isTargetConcrete) {
            val targetAddr = term.targetIndex
            val targetBBOpt = containingBlock(bbs, targetAddr)
            if (targetBBOpt.isDefined) {
              val targetBB = targetBBOpt.get
              assert(targetBB.firstEntry.index == targetAddr)
              term.relocate(targetBB)
              (bb ~+> targetBB)(false) :: l
            } else l
          } else l
        } else l
      }
    }
    edges.foreach { e => e._1.lastEntry }
    val edgesWithFallThrough = bbs.tail.foldLeft((edges, bbs.head))({
      case ((l, prev), next) => {
        if (prev.lastEntry.isTerminator) {
          val term = prev.lastEntry.asTerminator[T]
          if (term.isCall || term.isConditional)
            ((prev ~+> next)(true) :: l, next)
          else (l, next)
        } else (l, next)
      }
    })._1
    
    val graph = Graph.from(bbs, edgesWithFallThrough)
    val trimmedGraph = 
      if (!hasIndirectJump) {
        graph
      } else {
        graph filter graph.having(node = (bb => bb.value == entry || bb.hasPredecessors))
      }
  
    (hasIndirectJump, trimmedGraph, entry)
  }
}

class CFG[T <: Entry] (val parent : Procedure[T], items : Seq[T]) extends Iterable[BasicBlock[T]] {
  
  private val _internal = CFG.buildCFG(parent, items)
  private val _graph = _internal._2 

  val hasIndirectJump = _internal._1
  val entryBlock = _internal._3
  val blocks : SortedSet[BasicBlock[T]] = TreeSet[BasicBlock[T]]() ++ _graph.nodes.map(_.value)
  lazy val entries = blocks.foldLeft(List[BasicBlock[T]]()){ (a, b) => b::a }.flatMap(_.entries)
  def iterator = blocks.iterator
  
  def predecessors(bb : BasicBlock[T]) : Set[BasicBlock[T]] = (_graph.get(bb) <~|) map {_.value}
  def successors(bb : BasicBlock[T]) : Set[BasicBlock[T]] = (_graph.get(bb) ~>|) map {_.value}

}

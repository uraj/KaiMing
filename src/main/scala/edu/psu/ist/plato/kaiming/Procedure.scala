package edu.psu.ist.plato.kaiming

import edu.psu.ist.plato.kaiming.ir.Context

abstract class Procedure[A <: Arch] {

  def label: Label
  def cfg: Cfg[A, _ <: BBlock[A]]
  def name = label.name
  def entries: List[Entry[A]] = cfg.entries
  def deriveLabelForIndex(index: Long): Label = Label("_sub_" + index.toHexString)
  
}

object MachProcedure {
  
  import scala.collection.immutable.TreeSet

  import scalax.collection.Graph
  import scalax.collection.edge.LDiEdge
  import scalax.collection.edge.Implicits._
  
  private def split[A <: MachArch](unit : MachProcedure[A],
      entries : Seq[MachEntry[A]], pivots : Seq[Int]) = {
    val sortedPivots = (TreeSet[Int]() ++ pivots + 0 + entries.length).toVector
    require(sortedPivots.head == 0 && sortedPivots.last <= entries.length)
    (0 until (sortedPivots.length - 1)).foldRight(List[MachBBlock[A]]()) { 
      (i, blist) => {
        val index = entries(sortedPivots(i)).index
        new MachBBlock[A](unit, entries.slice(sortedPivots(i), sortedPivots(i + 1)),
            unit.deriveLabelForIndex(index)) :: blist
      }
    }
  }
  
  private def containingBlock[A <: MachArch](
      bbs : Traversable[MachBBlock[A]], index : Long) =
    bbs.find { bb => bb.lastEntry.index >= index && bb.firstEntry.index <= index }
  
  private def buildCFGImpl[A <: MachArch](parent : MachProcedure[A],
      entries : Seq[MachEntry[A]]) = {
    val sorted = entries.toVector.sorted[Entry[A]]
    val pivots = (0 until sorted.length).foldLeft(Vector[Int]()) {
      (vec, i) =>
        if (sorted(i).isTerminator) {
          val term = sorted(i).asInstanceOf[Terminator[A]]
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
      (has, bb) => has || (bb.lastEntry.isTerminator && bb.lastEntry.asTerminator.isIndirect)
    }
    val edges = bbs.foldLeft(List[LDiEdge[MachBBlock[A]]]()) {
      (l, bb) => {
        val in = bb.lastEntry
        if (in.isTerminator) {
          val term = in.asTerminator
          if (!term.isCall && !term.isIndirect && term.isTargetConcrete) {
            val targetAddr = term.targetIndex
            val targetBBOpt = containingBlock(bbs, targetAddr)
            if (targetBBOpt.isDefined) {
              val targetBB = targetBBOpt.get
              (bb ~+> targetBB)(false) :: l
            } else l
          } else l
        } else l
      }
    }
    edges.foreach { e => e._1.lastEntry.asTerminator.relocate(e._2) }
    val edgesWithFallThrough = bbs.tail.foldLeft((edges, bbs.head))({
      case ((l, prev), next) => {
        if (prev.lastEntry.isTerminator) {
          val term = prev.lastEntry.asTerminator
          if (term.isCall || term.isConditional)
            ((prev ~+> next)(true) :: l, next)
          else (l, next)
        } else ((prev ~+> next)(true) :: l, next)
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
  
  private def buildCFG[A <: MachArch](parent : MachProcedure[A], entries : Seq[MachEntry[A]]) = {
      val _t = buildCFGImpl(parent, entries)
      new MachCFG(parent, _t._3, _t._2, _t._1)
  }
  
}

abstract class MachProcedure[A <: MachArch](machEntries: Seq[MachEntry[A]]) extends Procedure[A] {
  
  def mach: Machine[A]
  val cfg = MachProcedure.buildCFG[A](this, machEntries)
  def liftCFGToIR(ctx: Context) = mach.liftToIR(ctx, cfg)
  
}
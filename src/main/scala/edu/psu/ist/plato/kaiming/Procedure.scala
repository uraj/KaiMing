package edu.psu.ist.plato.kaiming

abstract class Procedure[A <: Arch] {

  val label: Label
  val cfg: CFG[A]
  def name = label.name
  def entries: List[Entry[A]] = cfg.entries
  def deriveLabelForIndex(index: Long): Label
  
}

abstract class MachProcedure[A <: MachArch] extends Procedure[A] {
  
  val mach: Machine[A]
  
}

object MachProcedure {
  
  import scala.collection.immutable.TreeSet

  import scalax.collection.Graph
  import scalax.collection.edge.LDiEdge
  import scalax.collection.edge.Implicits._
  
    private def split[A <: MachArch](unit : MachProcedure[A],
      entries : Seq[Entry[A]], pivots : Seq[Int]) = {
    val sortedPivots = (TreeSet[Int]() ++ pivots + 0 + entries.length).toVector
    require(sortedPivots.head == 0 && sortedPivots.last <= entries.length)
    (0 until (sortedPivots.length - 1)).foldRight(List[BasicBlock[A]]()) { 
      (i, blist) => {
        val index = entries(sortedPivots(i)).index
        BasicBlock[A](unit, entries.slice(sortedPivots(i), sortedPivots(i + 1)),
            unit.deriveLabelForIndex(index)) :: blist
      }
    }
  }
  
  private def containingBlock[A <: MachArch](
      bbs : Traversable[BasicBlock[A]], index : Long) =
    bbs.find { bb => bb.lastEntry.index >= index && bb.firstEntry.index <= index }
  
  private def buildCFGImpl[A <: MachArch](parent : MachProcedure[A],
      entries : Seq[Entry[A]]) = {
    val sorted = entries.toVector.sorted[Entry[A]]
    val pivots = (0 until sorted.length).foldLeft(Vector[Int]()) {
      (vec, i) =>
        if (sorted(i).isTerminator) {
          val term = sorted(i).asInstanceOf[Entry.Terminator[A]]
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
    val edges = bbs.foldLeft(List[LDiEdge[BasicBlock[A]]]()) {
      (l, bb) => {
        val in = bb.lastEntry
        if (in.isTerminator) {
          val term = in.asTerminator
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
          val term = prev.lastEntry.asTerminator
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
  
  def buildCFG[A <: MachArch](parent : MachProcedure[A],
      entries : Seq[Entry[A]]) = {
      val _t = buildCFGImpl(parent, entries)
      new CFG(parent, _t._3, _t._2, _t._1)
  }
}

package edu.psu.ist.plato.kaiming

import scala.collection.immutable.{SortedSet, TreeSet}

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

import Arch.KaiMing
import ir.{Context, IRBBlock, IRBuilder}
import utils.Indexed

abstract class Cfg[A <: Arch, B <: BBlock[A]] extends Iterable[B] {
  
  val parent : Procedure[A]
  
  val graph: Graph[Int, LDiEdge]
  
  protected val blockIdMap: Map[B, Int]
  def blocks: Vector[B]
  
  def entryBlock = blocks(0)
  
  def entries = blocks.flatMap(_.entries)
  def iterator = blocks.iterator
  
  import scala.language.postfixOps
  
  def predecessors(bb: B): Set[B] = 
    (graph.get(blockIdMap(bb)) <~) map { x => blocks(x.from.value) }
  def successors(bb: B): Set[B] = 
    (graph.get(blockIdMap(bb)) ~>) map { x => blocks(x.to.value) }
  
  def predecessors(id: Int): Set[Int] = 
    (graph.get(id) <~) map(_.from.value)
  def successors(id: Int): Set[Int] = 
    (graph.get(id) ~>) map(_.to.value)
  
  def isConnected = graph.isConnected
  def belongingComponent(bb: B) = {
    blockIdMap.get(bb) match {
      case Some(id) =>    
        if (graph.contains(id)) {
          val inner = graph.get(id)
          graph.componentTraverser().find { _.nodes.contains(inner) }
        } else None
      case None => None
    }
  }

  object LEdgeImplicit extends scalax.collection.edge.LBase.LEdgeImplicits[Boolean]
  import LEdgeImplicit._
  def labeledPredecessors(bb: B): Set[(B, Boolean)] = 
    (graph.get(blockIdMap(bb)) <~) map { x => (blocks(x.from.value), x: Boolean) }
  def labeledSuccessors(bb: B): Set[(B, Boolean)] = 
    (graph.get(blockIdMap(bb)) ~>) map { x => (blocks(x.to.value), x: Boolean) }
  
  def labeledPredecessors(id: Int): Set[(Int, Boolean)] = 
    (graph.get(id) <~) map { x => (x.from.value, x: Boolean) }
  def labeledSuccessors(id: Int): Set[(Int, Boolean)] = 
    (graph.get(id) ~>) map { x => (x.to.value, x: Boolean) }

  def toDot = {
    val builder = new StringBuilder()
    builder.append("digraph {\n")
    graph.edges.foldLeft(builder) {
      (b, inner) => builder.append(s"""\t"${blocks(inner._1.value)}"->"${blocks(inner._2.value)}";\n""")
    }.append("}").toString
  }
  
}

object MachCfg {
  
  import scala.collection.immutable.TreeSet

  import scalax.collection.Graph
  import scalax.collection.edge.LDiEdge
  import scalax.collection.edge.Implicits._
  
  private def split[A <: MachArch](unit : MachProcedure[A],
      entries : Vector[MachEntry[A]], pivots : Seq[Int]) = {
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
  
  def apply[A <: MachArch](parent : MachProcedure[A]) = {
    val entries = parent.entries
    val sorted = entries.toVector.sorted[Indexed]
    val pivots =
      (0 until sorted.length).foldLeft(Vector[Int]()) {
        case (vec, i) =>
          val entry = sorted(i)
          if (entry.isTerminator) {
            val term = entry.asInstanceOf[Terminator[A]]
            val nvec = if (!term.isCall) vec :+ (i + 1) else vec
            if (term.isIntraprocedural && !term.isIndirect && term.isTargetConcrete) {
              val targetOpt = Indexed.binarySearch(sorted, term.targetIndex)
              if (targetOpt.isDefined)
                nvec :+ targetOpt.get
              else
                nvec
            }
            else
              nvec
          } else {
            vec
          }
      }
    val bbs = split(parent, entries, pivots).toVector
    val blocksWithId = bbs.zipWithIndex
    val blockIdMap = blocksWithId.toMap
    val hasIndirectJump = bbs.foldLeft(false) {
      (has, bb) => has || (bb.lastEntry.isTerminator && bb.lastEntry.asTerminator.isIndirect)
    }
    val edges = blocksWithId.foldLeft(List[LDiEdge[Int]]()) {
      case (l, (bb, id)) => {
        val in = bb.lastEntry
        if (in.isTerminator) {
          val term = in.asTerminator
          if (!term.isCall && !term.isIndirect && term.isTargetConcrete) {
            val targetAddr = term.targetIndex
            val targetBBOpt = Indexed.binarySearch(bbs, targetAddr)
            if (targetBBOpt.isDefined) {
              val targetBB = targetBBOpt.get
              (id ~+> targetBB)(false) :: l
            } else l
          } else l
        } else l
      }
    }
    
    edges.foreach { e => bbs(e._1).lastEntry.asTerminator.relocate(bbs(e._2)) }
    
    val edgesWithFallThrough = blocksWithId.tail.foldLeft((edges, blocksWithId.head))({
      case ((l, prev), next) => {
        if (prev._1.lastEntry.isTerminator) {
          val term = prev._1.lastEntry.asTerminator
          if (term.isCall || term.isConditional)
            ((prev._2 ~+> next._2)(true) :: l, next)
          else (l, next)
        } else ((prev._2 ~+> next._2)(true) :: l, next)
      }
    })._1
    
    val graph = Graph.from(0 until bbs.size, edgesWithFallThrough)
    new MachCfg(parent, graph, bbs, blockIdMap)
    
  }
  
}

class MachCfg[A <: MachArch] protected (val parent: MachProcedure[A],
    val graph: Graph[Int, LDiEdge], val blocks: Vector[MachBBlock[A]],
    protected val blockIdMap: Map[MachBBlock[A], Int]) extends Cfg[A, MachBBlock[A]]

object Cfg {

  import scalax.collection.Graph
  import scalax.collection.edge.LDiEdge
  
  sealed abstract class Loop[A <: Arch, B <: BBlock[A]] private (val header: Int,
      val body: Set[Int], val cfg: Cfg[A, B] ) {
    
    // We need this trait because Graph.Component is protected
    protected sealed trait Component {
      def nodes: Set[_ <: g.NodeT forSome {val g: Graph[Int, LDiEdge]}]
      def edges: Set[_ <: g.EdgeT forSome {val g: Graph[Int, LDiEdge]}]
    }
    
    val component: Component
    
    override def toString = {
      val b = new StringBuilder
      def bbToStr(bb: B) = {
        b.append(bb.label.name)
        b.append("[")
        b.append(bb.firstEntry.index.toHexString)
        b.append("]")
      }
      b.append("(");
      bbToStr(cfg.blocks(header))
      b.append("<" + body.size + ">: { ");
      for (bb <- body.map(cfg.blocks(_)).toVector.sorted[Indexed]) {
        bbToStr(bb)
        b.append(" ")
      }
      b.append("})");
      b.toString();
    }
    
  }
  
  object Loop {
    
    private def computeDominators[A <: Arch, B <: BBlock[A]](cfg: Cfg[A, B]) = {
      val allBBs = (0 until cfg.size).toSet
      val initDominators =
        allBBs.foldLeft(Map[Int, Set[Int]]()) {
          (map, bb) => map + (bb -> allBBs)
        }
      val singletons = allBBs.map { x => (x, Set(x)) }
      
      @scala.annotation.tailrec
      def computeDomImpl(input: (Boolean, Map[Int, Set[Int]])): Map[Int, Set[Int]] =
        input match {
          case (stop, in) =>
            if (stop) in
            else
            computeDomImpl(singletons.foldLeft((true, in)) {
              case ((stop, map), (bb, singleton)) => {
                val n = cfg.predecessors(bb).flatMap(map.get(_))
                val newDomSet = if (n.size == 0) singleton else n.reduce(_&_) + bb
                (stop && newDomSet == (in.get(bb).orNull), map + (bb -> newDomSet))
              }
            })
        }
      computeDomImpl((false, initDominators))
    }

    def detectLoops[A <: Arch, B <: BBlock[A]](cfg: Cfg[A, B], merge: Boolean = false): List[Loop[A, B]] = {
      val dominators = computeDominators(cfg)
      cfg.graph.componentTraverser().foldLeft(List[Loop[A, B]]()) {
        (l, subg) => {
          if (subg.nodes.size < 2) l else {
            val nodes = subg.nodes.map(_.value)
            val backEdges = dominators.filterKeys { nodes.contains(_) }.foldLeft(Set[(Int, Int)]()) {
              case (s, (k, v)) =>
                if (nodes.contains(k))
                  s ++ (cfg.successors(k) & v).map { x => (k, x) }
                else s
            }
            if (merge)
              backEdges.groupBy(_._2).foldLeft(l) { (s, group) =>
                val candidates = subg.nodes.filter(dominators.get(_).get.contains(group._1))
                (new Loop(group._1,
                  candidates.filter(_.pathTo(cfg.graph get group._1).isDefined).map(_.value),
                  cfg) {
                    val component = new Component {
                      def nodes = subg.nodes
                      def edges = subg.edges
                    }
                  })::s
              }
            else
              backEdges.foldLeft(l) { (s, x) =>
                val candidates = subg.nodes.filter(dominators.get(_).get.contains(x._2))
                (new Loop(x._2,
                  candidates.filter(_.pathTo(cfg.graph get x._2).isDefined).map(_.value),
                  cfg) {
                    val component = new Component {
                      def nodes = subg.nodes
                      def edges = subg.edges
                    }
                  })::s
              }
          }
        }
      }
    }

  }
}




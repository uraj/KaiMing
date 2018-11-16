package io.github.uraj.kaiming

import scala.collection.immutable.{SortedSet, TreeSet}

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

import ir.{Context, IRBBlock, IRBuilder}
import utils.Indexed

abstract class Cfg[A <: Arch] extends Iterable[BBlock[A]] {
  
  type BlockT <: BBlock[A]
  
  val parent : Procedure[A]
  
  val graph: Graph[Int, LDiEdge]
  
  protected val blockIdMap: Map[BlockT, Int]
  def blocks: Vector[BlockT]
  
  def entryBlock = blocks(0)
  
  def entries = blocks.flatMap(_.entries)
  def iterator = blocks.iterator
  
  import scala.language.postfixOps
  
  def predecessors(bb: BlockT): Set[BlockT] = 
    (graph.get(blockIdMap(bb)) <~) map { x => blocks(x.from.value) }
  def successors(bb: BlockT): Set[BlockT] = 
    (graph.get(blockIdMap(bb)) ~>) map { x => blocks(x.to.value) }
  
  def predecessors(id: Int): Set[Int] = 
    (graph.get(id) <~) map(_.from.value)
  def successors(id: Int): Set[Int] = 
    (graph.get(id) ~>) map(_.to.value)
  
  def isConnected = graph.isConnected
  def belongingComponent(bb: BlockT) = {
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
  def labeledPredecessors(bb: BlockT): Set[(BlockT, Boolean)] = 
    (graph.get(blockIdMap(bb)) <~) map { x => (blocks(x.from.value), x: Boolean) }
  def labeledSuccessors(bb: BlockT): Set[(BlockT, Boolean)] = 
    (graph.get(blockIdMap(bb)) ~>) map { x => (blocks(x.to.value), x: Boolean) }
  
  def labeledPredecessors(id: Int): Set[(Int, Boolean)] = 
    (graph.get(id) <~) map { x => (x.from.value, x: Boolean) }
  def labeledSuccessors(id: Int): Set[(Int, Boolean)] = 
    (graph.get(id) ~>) map { x => (x.to.value, x: Boolean) }

  def toDot = {
    val builder = new StringBuilder()
    builder.append("digraph {\n")
    builder.append(s"""\tsplines="ortho";\n""")
    graph.edges.toSeq.sortBy(x => (x._1.value , x._2.value)).foldLeft(builder) {
      (b, inner) =>
        builder.append(s"""\t"${blocks(inner._1.value)}"->"${blocks(inner._2.value)}";\n""")
    }.append("}").toString
  }
  
}

object MachCfg {
  
  import scala.collection.immutable.TreeSet

  import scalax.collection.Graph
  import scalax.collection.edge.LDiEdge
  import scalax.collection.edge.Implicits._
  
  private def split[A <: Arch](unit : Procedure[A],
      entries : Vector[Entry[A]], pivots : Seq[Int]) = {
    val sortedPivots = (TreeSet[Int]() ++ pivots + 0 + entries.length).toVector
    require(sortedPivots.head == 0 && sortedPivots.last <= entries.length)
    (0 until (sortedPivots.length - 1)).foldRight(List[BBlock[A]]()) { 
      (i, blist) => {
        val index = entries(sortedPivots(i)).index
        new BBlock[A](unit, entries.slice(sortedPivots(i), sortedPivots(i + 1)),
            unit.deriveLabelForIndex(index)) :: blist
      }
    }
  }
  
  private def containingBlock[A <: Arch](
      bbs : Traversable[BBlock[A]], index : Long) =
    bbs.find { bb => bb.lastEntry.index >= index && bb.firstEntry.index <= index }
  
  def apply[A <: Arch](parent : Procedure[A]) = {
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
    val bbs = split(parent, entries.asInstanceOf[Vector[Entry[A]]], pivots).toVector
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

class MachCfg[A <: Arch] protected (val parent: Procedure[A],
    val graph: Graph[Int, LDiEdge], val blocks: Vector[BBlock[A]],
    protected val blockIdMap: Map[BBlock[A], Int]) extends Cfg[A] {
  type BlockT = BBlock[A]
}

object Cfg {

  import scalax.collection.Graph
  import scalax.collection.edge.LDiEdge
  
  sealed abstract class Loop[A <: Arch] private (val header: Int,
       val backEdges: Set[(Int, Int)], val body: Set[Int], val cfg: Cfg[A]) {

    protected def nodes: Set[_ <: Graph[Int, LDiEdge]#NodeT]
    
    lazy val subgraph = {
      val _nodes = nodes.map(_.value)
      cfg.graph filter cfg.graph.having(node=(x => _nodes.contains(cfg.graph get x)))
    }
    
    def headerBlock = cfg.blocks(header)
    
    def toDot = {
      val builder = new StringBuilder()
      builder.append("digraph {\n")
      builder.append(s"""\tlabelloc="t";\n""")
      builder.append(s"""\tlabel="${cfg.parent.label}-${cfg.blocks(header).index.toHexString}";\n""")
      builder.append(s"""\tsplines="ortho";\n""")
      subgraph.edges.toSeq.sortBy(x => (x._1.value, x._2.value)).foldLeft(builder) {
        (b, inner) => builder.append(
            s"""\t"${cfg.blocks(inner._1.value)}"->"${cfg.blocks(inner._2.value)}";\n""")
      }.append("}").toString
  }

    
    override def toString = {
      val b = new StringBuilder
      def bbToStr(bb: cfg.BlockT) = {
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
    
    private def computeDominators[A <: Arch](nodes: Set[Int], cfg: Cfg[A]): Map[Int, Set[Int]] = {
      val allBBs = nodes
      val starts = allBBs.filter { x => (cfg.predecessors(x) - x).size == 0 }
      if (starts.size == 0) {
        val empty = Set[Int]()
        nodes.map(_ -> empty).toMap
      } else {
        val allExceptStarts = allBBs diff starts
        val singletons = allExceptStarts map { x => (x, Set(x)) }
        val initDominators =
          starts.foldLeft(
              allExceptStarts.foldLeft(Map[Int, Set[Int]]()) {
                (map, bb) => map + (bb -> allBBs)
              }
          ) { case (map, x) => map + (x -> Set(x)) }
      
        @scala.annotation.tailrec
        def computeDomImpl(input: (Boolean, Map[Int, Set[Int]])): Map[Int, Set[Int]] =
          input match {
            case (stop, in) =>
              if (stop) {
                val singularity = in.filter { case (k, v) => v.size == allBBs.size }
                if (singularity.size > 1) {
                  in ++ (singularity.keys.map((_ -> Set[Int]())))
                } else
                  in
              } else {
                computeDomImpl(singletons.foldLeft((true, in)) {
                  case ((stop, map), (bb, singleton)) => {
                    val n = cfg.predecessors(bb).map(map(_))
                    val newDomSet = if (n.size == 0) singleton else n.reduce(_&_) + bb
                    (stop && newDomSet == (in.get(bb).get), map + (bb -> newDomSet))
                  }
                })
              }
          }
        computeDomImpl((false, initDominators))
      }
    }

    def detectLoops[A <: Arch, B <: BBlock[A]](cfg: Cfg[A], merge: Boolean = false): List[Loop[A]] = {
      cfg.graph.componentTraverser().foldLeft(List[Loop[A]]()) {
        (l, subg) => {
          if (subg.nodes.size < 2) l else {
            val nodes = subg.nodes.map(_.value)
            val dominators = computeDominators(nodes, cfg)
            val backEdges = dominators.foldLeft(Set[(Int, Int)]()) {
              case (s, (k, v)) =>
                if (nodes.contains(k))
                  s ++ (cfg.successors(k) & v).map(k-> _)
                else s
            }
            if (merge)
              backEdges.groupBy(_._2).foldLeft(l) { (s, group) =>
                val candidates = subg.nodes.filter(dominators.get(_).get.contains(group._1))
                (new Loop(group._1, backEdges,
                  candidates.filter(_.pathTo(cfg.graph.get(group._1)).isDefined).map(_.value), cfg) {
                  def nodes = subg.nodes }
                )::s
              }
            else
              backEdges.foldLeft(l) { (s, x) =>
                val candidates = subg.nodes.filter(dominators.get(_).get.contains(x._2))
                (new Loop(x._2, backEdges,
                  candidates.filter(_.pathTo(cfg.graph get x._2).isDefined).map(_.value), cfg) {
                  def nodes = subg.nodes }
                )::s
              }
          }
        }
      }
    }

  }
}




package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming.{MachArch, BBlock}
import edu.psu.ist.plato.kaiming.Arch.KaiMing
import edu.psu.ist.plato.kaiming.utils.Indexed

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

sealed abstract class Loop[A <: MachArch] private (val header: Int,
    val body: Set[Int], val cfg: IRCfg[A]) {
  
  protected sealed trait Component {
    def nodes: Set[_ <: g.NodeT forSome {val g: Graph[Int, LDiEdge]}]
    def edges: Set[_ <: g.EdgeT forSome {val g: Graph[Int, LDiEdge]}]
  }
  
  val component: Component
  
  override def toString = {
    val b = new StringBuilder
    def bbToStr(bb: IRBBlock[A]) = {
      b.append(bb.label.name)
      b.append("[")
      b.append(bb.firstEntry.host.index.toHexString)
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
  
  private def computeDominators[A <: MachArch](cfg: IRCfg[A]) = {
    val allBBs = (0 until cfg.size).toSet
    val initDominators =
      allBBs.foldLeft(Map[Int, Set[Int]]()) {
        (map, bb) => map + (bb -> allBBs)
      }
    val singletons = allBBs.map { x => (x, Set(x)) }
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
  
  def detectLoops[A <: MachArch](cfg: IRCfg[A], merge: Boolean = false): Vector[Loop[A]] = {
    lazy val dominators = computeDominators(cfg)
    cfg.graph.componentTraverser().foldLeft(Vector[Loop[A]]()) {
      (l, subg) => {
        if (subg.nodes.size < 2) l else {
          val nodeMap = subg.nodes.map(_.value).zipWithIndex.toMap
          val backEdges = dominators.filterKeys { nodeMap.contains(_) }.foldLeft(Set[(Int, Int)]()) {
            case (l, (k, v)) =>
              if (nodeMap.contains(k))
                l ++ (cfg.successors(k) & v).map { x => (k, x) }
              else l
          }
          val n = subg.nodes.size
          val reach = Array.ofDim[Int](n, n)
          subg.edges.foreach { x => reach(nodeMap(x.from.value))(nodeMap(x.to.value)) = 1 }
          val indices = 0 until subg.nodes.size
          for (i <- indices; j <- indices; k <- indices) {
            reach(i)(j) |= (reach(i)(k) & reach(k)(j))
          }
          l ++ {
            if (merge)
              backEdges.groupBy(_._2).foldLeft(List[Loop[A]]()) { (s, group) =>
                val candidates = subg.nodes.filter(dominators.get(_).get.contains(group._1))
                (new Loop(group._1,
                  candidates.filter {
                    y => reach(nodeMap(y))(nodeMap(group._1)) != 0
                  }.map(_.value), cfg) {
                    val component = new Component {
                      def nodes = subg.nodes
                      def edges = subg.edges
                    }
                  })::s
              }
            else
              backEdges.foldLeft(List[Loop[A]]()) { (s, x) =>
                val candidates = subg.nodes.filter(dominators.get(_).get.contains(x._2))
                (new Loop(x._2,
                  candidates.filter {
                    y => reach(nodeMap(y))(nodeMap(x._1)) != 0
                  }.map(_.value), cfg) {
                    val component = new Component {
                      def nodes = subg.nodes
                      def edges = subg.edges
                    }
                  })::s
              }.toVector
          }
        }
      }
    }
  }
  
}
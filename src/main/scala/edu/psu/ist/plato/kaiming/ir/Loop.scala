package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming.BBlock
import edu.psu.ist.plato.kaiming.Arch.KaiMing
import edu.psu.ist.plato.kaiming.MachArch
import edu.psu.ist.plato.kaiming.utils.Indexed

case class Loop[A <: MachArch] private (header: IRBBlock[A], body: Set[IRBBlock[A]], cfg: IRCfg[A]) {
  
  override def toString = {
    val b = new StringBuilder
    def bbToStr(bb: IRBBlock[A]) = {
      b.append(bb.label.name)
      b.append("[")
      b.append(bb.firstEntry.host.index.toHexString)
      b.append("]")
    }
    b.append("(");
    bbToStr(header)
    b.append("<" + body.size + ">: { ");
    for (bb <- body.toVector.sorted[Indexed]) {
      bbToStr(bb)
      b.append(" ")
    }
    b.append("})");
    b.toString();
  }
  
}

object Loop {
  
  def detectOuterLoops[A <: MachArch](cfg: IRCfg[A]): List[Loop[A]] = {
    val loops = detectLoops(cfg)
    loops.groupBy(_.header).foldLeft(List[Loop[A]]()) {
      case (list, (header, group)) =>
        Loop(header, group.foldLeft(Set[IRBBlock[A]]())(_ | _.body), cfg)::list
    }
  }
  
  def detectLoops[A <: MachArch](cfg: IRCfg[A]): List[Loop[A]] = {
    val indices = (0 until cfg.size)
    val allBBs = indices.toSet
    val initDominators =
      allBBs.foldLeft(Map[Int, Set[Int]]()) {
        (map, bb) => map + (bb -> allBBs)
      }
    val singletons = allBBs.map { x => (x, Set(x)) }
    def computeDoms(input: (Boolean, Map[Int, Set[Int]]))
     : Map[Int, Set[Int]] = input match {
      case (stop, in) =>
        if (stop) in
        else
          computeDoms(singletons.foldLeft((true, in)) {
            case ((stop, map), (bb, singleton)) => {
              val n = cfg.predecessors(bb).flatMap(map.get(_))
              val newDomSet = if (n.size == 0) singleton else n.reduce(_&_) + bb
              (stop && newDomSet == (in.get(bb).orNull), map + (bb -> newDomSet))
            }
          })
      }
    val dominators = computeDoms((false, initDominators))
    val backEdges = dominators.foldLeft(Set[(Int, Int)]()) {
      case (l, (k, v)) => l ++ (cfg.successors(k) & v).map { x => (k, x) }
    }
    val n = allBBs.size
    val reach = Array.ofDim[Boolean](n, n)
    cfg.graph.edges.foreach { x => reach(x.from.value)(x.to.value) = true }
    
    for (i <- indices; j <- indices; k <- indices) {
      if (reach(i)(k) && reach(k)(j)) reach(i)(j) = true
    }
    
    backEdges.foldLeft(List[Loop[A]]()) {
      (s, x) =>
        val candidates = allBBs.filter { dominators.get(_).get.contains(x._2) }
        (new Loop(cfg.blocks(x._2),
            candidates.filter(reach(_)(x._2)).map(cfg.blocks(_)), cfg))::s
    }
  }
  
}
package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming.Arch.KaiMing

case class Loop private (header: IRBBlock, body: Set[IRBBlock], cfg: IRCfg) {
  
  override def toString = {
    val b = new StringBuilder()
    def bbToStr(bb: IRBBlock) = {
      b.append(bb.label.name)
      b.append("[")
      b.append(bb.firstEntry.host.index.toHexString)
      b.append("]")
    }
    b.append("(");
    bbToStr(header)
    b.append(": { ");
    for (bb <- body) {
      bbToStr(bb)
      b.append(" ")
    }
    b.append("})");
    b.toString();
  }
  
}

object Loop {
  
  def detectLoops(cfg: IRCfg): Set[Loop] = {
    val allBBs = cfg.blocks.toSet
    val initDominators =
      allBBs.foldLeft(Map[IRBBlock, Set[IRBBlock]]()) {
        (map, bb) => map + (bb -> allBBs)
      }
    val singletons = allBBs.map { x => (x, Set(x)) }
    def computeDoms(input: (Boolean, Map[IRBBlock, Set[IRBBlock]]))
     : Map[IRBBlock, Set[IRBBlock]] = input match {
      case (stop, in) =>
        if (stop) in
        else
          computeDoms(singletons.foldLeft((true, in)) {
            case ((stop, map), (bb, singleton)) => {
              val n = cfg.predecessors(bb).map(x => map.get(x)).flatten
              val newDomSet = if (n.size == 0) singleton else n.reduce(_&_) + bb
              (stop && newDomSet == (in.get(bb).orNull), map + (bb -> newDomSet))
            }
          })
      }
    val newDominators = computeDoms((false, initDominators))
    val backEdges = newDominators.foldLeft(List[(IRBBlock, IRBBlock)]()) {
      case (l, (k, v)) => l ++ (cfg.successors(k) & v).map { x => (k, x) }
    }
    backEdges.foldLeft(Set[Loop]()) { 
      (s, x) => s + 
        findLoopNodes(cfg, x, 
            allBBs.filter(newDominators.get(_).get.contains(x._2)))
    }
  }
  
  private def findLoopNodes(cfg: IRCfg,
      backEdge: (IRBBlock, IRBBlock),
      candidates: Set[IRBBlock]): Loop = {
    val visited = Set[IRBBlock](backEdge._2)
    new Loop(
        backEdge._2,
        candidates.foldLeft(Set[IRBBlock]()) {
          (set, bb) =>
            if (set.contains(bb))
              set
            else
              reachable(cfg, bb, backEdge._1, candidates, set, visited)._2 
    } ++ Set(backEdge._1, backEdge._2), cfg)
  }
  
  private def reachable(cfg: IRCfg, start: IRBBlock, end: IRBBlock,
      candidates: Set[IRBBlock], ret: Set[IRBBlock],
      visited: Set[IRBBlock]): (Boolean, Set[IRBBlock]) = {
    if (start == end)
      (true, ret)
    else {
      val (c, s) = cfg.successors(start).foldLeft((false, ret)) {
        case ((canReach, set), bb) =>
          if (!candidates.contains(bb))
            (canReach, set)
          else if (visited.contains(bb))
            (canReach || ret.contains(bb), set)
          else { 
            val (c, s) = reachable(cfg, bb, end, candidates, ret, visited + start)
            (canReach || c, s)
          }
      }
      (c, if (c) s + start else s)
    }
  }
  
}
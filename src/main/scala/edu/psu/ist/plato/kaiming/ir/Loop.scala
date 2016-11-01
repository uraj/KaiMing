package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming.BBlock
import edu.psu.ist.plato.kaiming.Arch.KaiMing

case class Loop private (header: IRBBlock, body: Set[IRBBlock], cfg: IRCfg) {
  
  override def toString = {
    val b = new StringBuilder
    def bbToStr(bb: IRBBlock) = {
      b.append(bb.label.name)
      b.append("[")
      b.append(bb.firstEntry.host.index.toHexString)
      b.append("]")
    }
    b.append("(");
    bbToStr(header)
    b.append("<" + body.size + ">: { ");
    for (bb <- body.toVector.sorted[BBlock[KaiMing]]) {
      bbToStr(bb)
      b.append(" ")
    }
    b.append("})");
    b.toString();
  }
  
}

object Loop {
  
  def detectOuterLoops(cfg: IRCfg): List[Loop] = {
    val loops = detectLoops(cfg)
    loops.groupBy(_.header).foldLeft(List[Loop]()) {
      case (list, (header, group)) =>
        Loop(header, group.foldLeft(Set[IRBBlock]())(_ | _.body), cfg)::list
    }
  }
  
  def detectLoops(cfg: IRCfg): List[Loop] = {
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
              val n = cfg.predecessors(bb).flatMap(map.get(_))
              val newDomSet = if (n.size == 0) singleton else n.reduce(_&_) + bb
              (stop && newDomSet == (in.get(bb).orNull), map + (bb -> newDomSet))
            }
          })
      }
    val dominators = computeDoms((false, initDominators))
    val backEdges = dominators.foldLeft(Set[(IRBBlock, IRBBlock)]()) {
      case (l, (k, v)) => l ++ (cfg.successors(k) & v).map { x => (k, x) }
    }
    backEdges.foldLeft(List[Loop]()) { 
      (s, x) => 
        findLoopNodes(cfg, x, 
            allBBs.filter(dominators.get(_).get.contains(x._2)))::s
    }
  }
  
  private def findLoopNodes(cfg: IRCfg, backEdge: (IRBBlock, IRBBlock),
      candidates: Set[IRBBlock]): Loop = {
    val visited = Set[IRBBlock](backEdge._2)
    new Loop(
        backEdge._2,
        candidates.foldLeft(Set[IRBBlock](backEdge._1, backEdge._2)) {
          (set, bb) =>
            if (set.contains(bb))
              set
            else
              reachable(cfg, bb, backEdge._1, candidates, set, visited)._2 
    }, cfg)
  }
  
  private def reachable(cfg: IRCfg, start: IRBBlock, end: IRBBlock,
      candidates: Set[IRBBlock], reached: Set[IRBBlock],
      visited: Set[IRBBlock]): (Boolean, Set[IRBBlock]) = {
    if (start == end)
      (true, reached)
    else if (visited.contains(start)) {
      (false, reached)
    }
    else {
      val (c, s) = cfg.successors(start).foldLeft((false, reached)) {
        case ((canReach, reached), bb) =>
          if (canReach)
            (true, reached)
          else if (!candidates.contains(bb))
            (canReach, reached)
          else { 
            val (c, s) = reachable(cfg, bb, end, candidates, reached, visited + start)
            (canReach || c, s)
          }
      }
      (c, if (c) s + start else s)
    }
  }
  
}
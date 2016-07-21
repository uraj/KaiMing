package edu.psu.ist.plato.kaiming

case class Loop[A <: Arch] private (header: BBlock[A], body: Set[BBlock[A]]) {
  
  override def toString = {
    val b = new StringBuilder()
    b.append("(");
    b.append(header.label);
    b.append(": [ ");
    body.foreach {x => { b.append(x.label); b.append(" "); }}
    b.append("])");
    b.toString();
  }
  
}

object Loop {
  
  def detectLoops[T <: Arch](cfg: CFG[T]): List[Loop[T]] = {
    val allBBs = cfg.blocks.toSet
    val initDominators = allBBs.foldLeft(Map[BBlock[T], Set[BBlock[T]]]()) {
      (map, bb) => map + (bb -> allBBs)
    }
    val singletons = allBBs.map { x => (x, Set(x)) }
    def computeDoms(input: (Boolean, Map[BBlock[T], Set[BBlock[T]]]))
     : Map[BBlock[T], Set[BBlock[T]]] = input match {
      case (stop, in) =>
      if (stop)
        in
      else
        computeDoms(singletons.foldLeft((true, in)) {
          case ((stop, map), (bb, singleton)) => {
            val n = cfg.predecessors(bb).map(x => in.get(x)).flatten
            val newDomSet = if (n.size == 0) singleton else n.reduce(_&_) + bb
            (stop && newDomSet.equals(in.get(bb).orNull), in + (bb -> newDomSet))
          }
        })
    }
    val newDominators = computeDoms((false, initDominators))
    val backEdges = newDominators.foldLeft(List[(BBlock[T], BBlock[T])]()) {
      case (l, (k, v)) => l ++ (cfg.successors(k) & v).map { x => (k, x) }
    }
    backEdges.map { 
      x => 
        findLoopNodes(cfg, x, 
            allBBs.filter {
              y => newDominators.get(y).orNull.contains(x._2)
            })}
  }
  
  private def findLoopNodes[T <: Arch](cfg: CFG[T],
      backEdge: (BBlock[T], BBlock[T]),
      candidates: Set[BBlock[T]]): Loop[T] = {
    new Loop(
        backEdge._2,
        candidates.foldLeft(Set(backEdge._1, backEdge._2)) {
          (set, bb) =>
            if (set.contains(bb))
              set
            else
              reachable(cfg, bb, backEdge._1, candidates, set, Set[BBlock[T]]())._2 
    })
  }
  
  private def reachable[T <: Arch](cfg: CFG[T], start: BBlock[T], end: BBlock[T],
      candidates: Set[BBlock[T]], ret: Set[BBlock[T]],
      visited: Set[BBlock[T]]): (Boolean, Set[BBlock[T]]) = {
    if (start.equals(end))
      (true, ret + start)
    else {
      cfg.successors(start).foldLeft((false, ret)) {
        case ((canReach, set), bb) =>
          if (!candidates.contains(bb))
            (canReach, set)
          else if (visited.contains(bb))
            (canReach || ret.contains(bb), set)
          else 
            reachable(cfg, bb, end, candidates, ret, visited + start)
      }
    }
  }
  
}
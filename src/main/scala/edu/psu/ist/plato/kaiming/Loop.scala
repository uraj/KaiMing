package edu.psu.ist.plato.kaiming

case class Loop[A <: Arch] private (header: BasicBlock[A], body: Set[BasicBlock[A]]) {
  
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
    val initDominators = allBBs.foldLeft(Map[BasicBlock[T], Set[BasicBlock[T]]]()) {
      (map, bb) => map + (bb -> allBBs)
    }
    val singletons = allBBs.map { x => (x, Set(x)) }
    def computeDoms(input: (Boolean, Map[BasicBlock[T], Set[BasicBlock[T]]]))
     : Map[BasicBlock[T], Set[BasicBlock[T]]] = input match {
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
    val backEdges = newDominators.foldLeft(List[(BasicBlock[T], BasicBlock[T])]()) {
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
      backEdge: (BasicBlock[T], BasicBlock[T]),
      candidates: Set[BasicBlock[T]]): Loop[T] = {
    new Loop(
        backEdge._2,
        candidates.foldLeft(Set(backEdge._1, backEdge._2)) {
          (set, bb) =>
            if (set.contains(bb))
              set
            else
              reachable(cfg, bb, backEdge._1, candidates, set, Set[BasicBlock[T]]())._2 
    })
  }
  
  private def reachable[T <: Arch](cfg: CFG[T], start: BasicBlock[T], end: BasicBlock[T],
      candidates: Set[BasicBlock[T]], ret: Set[BasicBlock[T]],
      visited: Set[BasicBlock[T]]): (Boolean, Set[BasicBlock[T]]) = {
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
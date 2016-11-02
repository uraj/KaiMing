package edu.psu.ist.plato.kaiming.ir.dataflow

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.KaiMing
import edu.psu.ist.plato.kaiming.MachArch
import edu.psu.ist.plato.kaiming.ir._

abstract class FlowInsensitiveProblem[T, A <: MachArch](ctx: Context[A]) {
  
  protected def getInitialState: T
  protected def process(e: Stmt, in: T): T
  
  protected final lazy val solve =
    ctx.entries.foldLeft(getInitialState){ (state, stmt) => process(stmt, state) }
}

sealed trait Direction
case object Forward extends Direction
case object Backward extends Direction

abstract class PathInsensitiveProblem[T, A <: MachArch](ctx: Context[A], dir: Direction, maxIterMultiplier: Int) {
  
  private type BB = BBlock[KaiMing]
  
  protected def getInitialEntryState(bb: BB): T
  protected def transfer(bb: BB, in: T): T
  protected def confluence(dataSet: Set[T]): T
  
  protected final lazy val solve = {
    val cfg = ctx.cfg
    val stop = maxIterMultiplier * cfg.size.toLong
    val (initEntryMap, initExitMap) = cfg.foldLeft((Map[BB, T](), Map[BB, T]())) {
        (m, b) => {
          val init = getInitialEntryState(b)
          (m._1 + (b -> init), m._2 + (b -> transfer(b, init)))
        }
      }
    def solveImpl(round: Long, entryMap: Map[BB, T], exitMap: Map[BB, T]): Map[BB, T] = {
      require(round <= stop, "Problem cannot be solved with in limited time: " + round + "/" + stop)
      val (updated, entryMapNew, exitMapNew) =
        cfg.foldLeft((false, entryMap, exitMap)) {
          case ((dirty, enM, exM), bb) => {
            val toConfluence = dir match {
              case Forward => cfg.predecessors(bb).foldLeft(Set[T]()) {
                  (set, pred) => set + exM.get(pred).get
                }
              case Backward => cfg.successors(bb).foldLeft(Set[T]()) {
                  (set, succ) => set + exM.get(succ).get
                }
            }
            val entryNew = confluence(toConfluence)
            val dirtyNew = entryNew != enM.get(bb).get
            if (dirtyNew)
              (true, enM + (bb -> entryNew),
                  exM + (bb -> transfer(bb, entryNew)))
            else
              (dirty, enM, exM)
          }
        }
        if (updated)
          solveImpl(round + 1, entryMapNew, exitMapNew)
        else
          exitMapNew
      }
    solveImpl(0L, initEntryMap, initExitMap)
  }
  
}

import scala.collection.BitSet

abstract class DataFlowProblem[A <: MachArch](ctx: Context[A],
    dir: Direction, maxIterMultiplier: Int)
    extends PathInsensitiveProblem[BitSet, A](ctx, dir, maxIterMultiplier) {

  private type BB = BBlock[KaiMing]
  
  protected def gen(bb: BB): BitSet
  protected def kill(bb: BB): BitSet
  protected val initialState: BitSet
  
  private val _genMap = ctx.cfg.foldLeft(Map[BB, BitSet]()) {
    (m, bb) => m + (bb -> gen(bb))
  }
  private val _killMap = ctx.cfg.foldLeft(Map[BB, BitSet]()) {
    (m, bb) => m + (bb -> kill(bb))
  }

  override protected final def getInitialEntryState(bb: BB) = initialState
  
  override protected final def transfer(bb: BB, in: BitSet) =
    (in &~ _killMap.get(bb).get) | _genMap.get(bb).get
  
}

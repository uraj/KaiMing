package edu.psu.ist.plato.kaiming.ir.dataflow

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.KaiMing
import edu.psu.ist.plato.kaiming.MachArch
import edu.psu.ist.plato.kaiming.ir._

abstract class FlowInsensitiveProblem[A <: MachArch, T](ctx: Context[A]) {
  
  protected def getInitialState: T
  protected def process(e: Stmt[A], in: T): T
  
  protected final lazy val solve =
    ctx.entries.foldLeft(getInitialState){ (state, stmt) => process(stmt, state) }
}

sealed trait Direction
case object Forward extends Direction
case object Backward extends Direction

abstract class PathInsensitiveProblem[T](ctx: Context[_ <: MachArch], dir: Direction, maxIterMultiplier: Int) {
  
  protected def getInitialEntryState(bid: Int): T
  protected def transfer(bid: Int, in: T): T
  protected def confluence(dataSet: Set[T]): T
  
  protected final lazy val solve = {
    val cfg = ctx.cfg
    val stop = maxIterMultiplier * cfg.size.toLong
    val (initEntryMap, initExitMap) = (0 until cfg.size).foldLeft((Map[Int, T](), Map[Int, T]())) {
        (m, b) => {
          val init = getInitialEntryState(b)
          (m._1 + (b -> init), m._2 + (b -> transfer(b, init)))
        }
      }
    def solveImpl(round: Long, entryMap: Map[Int, T], exitMap: Map[Int, T]): Map[Int, T] = {
      require(round <= stop, "Problem cannot be solved with in limited time: " + round + "/" + stop)
      val (updated, entryMapNew, exitMapNew) =
        (0 until cfg.size).foldLeft((false, entryMap, exitMap)) {
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

abstract class DataFlowProblem(ctx: Context[_ <: MachArch],
    dir: Direction, maxIterMultiplier: Int)
    extends PathInsensitiveProblem[BitSet](ctx, dir, maxIterMultiplier) {

  private type BB = BBlock[KaiMing]
  
  protected def gen(bid: Int): BitSet
  protected def kill(bid: Int): BitSet
  protected val initialState: BitSet
  
  private val _genMap = (0 until ctx.cfg.size).foldLeft(Map[Int, BitSet]()) {
    (m, bb) => m + (bb -> gen(bb))
  }
  private val _killMap = (0 until ctx.cfg.size).foldLeft(Map[Int, BitSet]()) {
    (m, bb) => m + (bb -> kill(bb))
  }

  override protected final def getInitialEntryState(bid: Int) = initialState
  
  override protected final def transfer(bid: Int, in: BitSet) =
    (in &~ _killMap.get(bid).get) | _genMap.get(bid).get
  
}

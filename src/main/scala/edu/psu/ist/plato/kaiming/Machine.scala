package edu.psu.ist.plato.kaiming

import edu.psu.ist.plato.kaiming.Arch._

import edu.psu.ist.plato.kaiming.ir.Stmt
import edu.psu.ist.plato.kaiming.ir.Context

import edu.psu.ist.plato.kaiming.aarch64.AArch64Machine

abstract class Machine[A <: MachArch] {

  val returnRegister: MachRegister[A]
  val wordSizeInBits: Int
  val registers: Set[MachRegister[A]]
  
  protected case class IRBuilder(private val start: Long, private val content: List[Stmt]) {
    
    def +(s: Stmt) = IRBuilder(start, s::content)
    def get = content.reverse
    def nextIndex = start + content.size
    
  }
  
  protected def toIRStatements(ctx: Context, inst: MachEntry[A], builder: IRBuilder): IRBuilder
  
  def liftToIR(ctx: Context, cfg: MachCFG[A]) : Cfg.IRCfg = {
    import scalax.collection.Graph
    import scalax.collection.edge.Implicits._
    import scalax.collection.edge.LDiEdge
    import edu.psu.ist.plato.kaiming.ir.JmpStmt
    
    val (bbs, bbmap, nStmts) = cfg.blocks.foldLeft(
      (List[BBlock[KaiMing]](), Map[MachBBlock[A], BBlock[KaiMing]](), 0L)) {
        case ((bblist, map, start), bb) => {
          val builder = bb.foldLeft(IRBuilder(start, Nil)) {
            (b, inst) => toIRStatements(ctx, inst, b)
          }
          val irlist = builder.get
          val irbb = new BBlock[KaiMing](ctx, irlist, Label("L_" + bblist.size))
          (irbb::bblist, map + (bb -> irbb), builder.nextIndex)
        }
      }
    val graph = cfg.blocks.foldLeft(Graph[BBlock[KaiMing], LDiEdge]()) {
      (g, bb) => {
        val irbb = bbmap.get(bb).get
        val edges = cfg.labeledPredecessors(bb).map { 
          case (pred, fallthrough) => {
            val irbbPred = bbmap.get(pred).get
            if (!fallthrough) {
              irbbPred.lastEntry.asInstanceOf[JmpStmt].relocate(irbb)
            }
            (irbbPred ~+> irbb)(fallthrough)
          }
        }
        if (edges.isEmpty) g + irbb else g ++ edges 
      }
    }
    new Cfg.IRCfg(ctx, bbmap.get(cfg.entryBlock).get, graph, cfg.hasIndirectJump)
  }

}

trait MachFlag[A <: MachArch] extends enumeratum.EnumEntry {
  
  def name: String
  def index: Int
  
}

abstract class MachRegister[A <: MachArch] {
  
  def name: String
  def sizeInBits: Int
  def containingRegister: MachRegister[A]
  def subsumedRegisters: Set[MachRegister[A]]
  
  override def equals(that: Any): Boolean
  override def hashCode: Int
  
}

package edu.psu.ist.plato.kaiming

import edu.psu.ist.plato.kaiming.Arch._
import edu.psu.ist.plato.kaiming.ir._

import edu.psu.ist.plato.kaiming.aarch64.AArch64Machine

abstract class Machine[A <: MachArch] {

  val returnRegister: MachRegister[A]
  val wordSizeInBits: Int
  val registers: Set[MachRegister[A]]
  
  protected case class IRBuilder(val ctx: Context[A], private val start: Long, private val content: List[Stmt]) {
    
    def get = content.reverse
    def nextIndex = start + content.size
    
    def assign(host: MachEntry[_ <: MachArch], definedLval: Lval, usedRval: Expr) =
      IRBuilder(ctx, start, AssignStmt(nextIndex, host, definedLval, usedRval)::content)
      
    def store(host: MachEntry[_ <: MachArch], storeTo: Expr, storedExpr: Expr) =
      IRBuilder(ctx, start, StStmt(nextIndex, host, storeTo, storedExpr)::content)
      
    def jump(host: MachEntry[A] with Terminator[A] forSome { type A <: MachArch },
        cond: Expr, target: Expr) = 
      IRBuilder(ctx, start, JmpStmt(nextIndex, host, cond, target)::content)
      
    def call(host: MachEntry[A] with Terminator[A] forSome { type A <: MachArch },
        target: Expr) =
      IRBuilder(ctx, start, CallStmt(nextIndex, host, target)::content)
      
    def load(host: MachEntry[_ <: MachArch], definedLval: Lval, loadFrom: Expr) =
      IRBuilder(ctx, start, LdStmt(nextIndex, host, definedLval, loadFrom)::content)
      
    def select(host: MachEntry[_ <: MachArch], definedLval: Lval, condition: Expr,
        trueValue: Expr, falseValue: Expr) =
      IRBuilder(ctx, start, SelStmt(nextIndex, host, definedLval, condition, trueValue, falseValue)::content)
      
    def ret(host: MachEntry[_ <: MachArch], target: Expr) =
      IRBuilder(ctx, start, RetStmt(nextIndex, host, target)::content)
  }
  
  protected def toIRStatements(inst: MachEntry[A], builder: IRBuilder): IRBuilder
  
  def liftToIR(ctx: Context[A], cfg: MachCFG[A]): IRCfg[A] = {
    import scalax.collection.Graph
    import scalax.collection.edge.Implicits._
    import scalax.collection.edge.LDiEdge
    import edu.psu.ist.plato.kaiming.ir.JmpStmt
    val (bbs, bbmap, nStmts) = cfg.blocks.foldLeft(
      (List[IRBBlock](), Map[MachBBlock[A], IRBBlock](), 0L)) {
        case ((bblist, map, start), bb) => {
          val builder = bb.foldLeft(IRBuilder(ctx, start, Nil)) {
            (b, inst) => toIRStatements(inst, b)
          }
          val irlist = builder.get
          val irbb = new IRBBlock(ctx, irlist, Label("L_" + bblist.size))
          (irbb::bblist, map + (bb -> irbb), builder.nextIndex)
        }
      }
    val graph = cfg.blocks.foldLeft(Graph[IRBBlock, LDiEdge]()) {
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
    new IRCfg(ctx, bbmap.get(cfg.entryBlock).get, graph, cfg.hasIndirectJmp,
        bbmap.toList.map { case (a, b) => (b, a) }.toMap, cfg.hasDanglingJump)
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

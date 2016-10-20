package edu.psu.ist.plato.kaiming

import edu.psu.ist.plato.kaiming.Arch._
import edu.psu.ist.plato.kaiming.ir._

import edu.psu.ist.plato.kaiming.aarch64.AArch64Machine

abstract class Machine[A <: MachArch] {

  val returnRegister: MachRegister[A]
  val wordSizeInBits: Int
  val registers: Set[MachRegister[A]]
  
  protected case class IRBuilder(private val start: Long, private val content: List[Stmt]) {
    
    def get = content.reverse
    def nextIndex = start + content.size
    
    def buildAssign(host: MachEntry[_ <: MachArch], definedLval: Lval, usedRval: Expr) =
      IRBuilder(start, AssignStmt(nextIndex, host, definedLval, usedRval)::content)
      
    def buildSt(host: MachEntry[_ <: MachArch], storeTo: Expr, storedExpr: Expr) =
      IRBuilder(start, StStmt(nextIndex, host, storeTo, storedExpr)::content)
      
    def buildJmp(host: MachEntry[A] with Terminator[A] forSome { type A <: MachArch },
        cond: Expr, target: Expr) = 
      IRBuilder(start, JmpStmt(nextIndex, host, cond, target)::content)
      
    def buildCall(host: MachEntry[A] with Terminator[A] forSome { type A <: MachArch },
        target: Expr) =
      IRBuilder(start, CallStmt(nextIndex, host, target)::content)
      
    def buildLd(host: MachEntry[_ <: MachArch], definedLval: Lval, loadFrom: Expr) =
      IRBuilder(start, LdStmt(nextIndex, host, definedLval, loadFrom)::content)
      
    def buildSel(host: MachEntry[_ <: MachArch], definedLval: Lval, condition: Expr,
        trueValue: Expr, falseValue: Expr) =
      IRBuilder(start, SelStmt(nextIndex, host, definedLval, condition, trueValue, falseValue)::content)
      
    def buildRet(host: MachEntry[_ <: MachArch], target: Expr) =
      IRBuilder(start, RetStmt(nextIndex, host, target)::content)
  }
  
  protected def toIRStatements(ctx: Context, inst: MachEntry[A], builder: IRBuilder): IRBuilder
  
  def liftToIR(ctx: Context, cfg: MachCFG[A]): IRCfg = {
    import scalax.collection.Graph
    import scalax.collection.edge.Implicits._
    import scalax.collection.edge.LDiEdge
    import edu.psu.ist.plato.kaiming.ir.JmpStmt
    
    val (bbs, bbmap, nStmts) = cfg.blocks.foldLeft(
      (List[IRBBlock](), Map[MachBBlock[A], IRBBlock](), 0L)) {
        case ((bblist, map, start), bb) => {
          val builder = bb.foldLeft(IRBuilder(start, Nil)) {
            (b, inst) => toIRStatements(ctx, inst, b)
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

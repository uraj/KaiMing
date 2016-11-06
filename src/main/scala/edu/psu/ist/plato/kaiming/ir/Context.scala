package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.KaiMing

import edu.psu.ist.plato.kaiming.ir.dataflow.PathInsensitiveProblem
import edu.psu.ist.plato.kaiming.ir.dataflow.Forward

import edu.psu.ist.plato.kaiming.utils.Exception

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.edge.LDiEdge

class IRBBlock[A <: MachArch](override val parent: Context[A], override val entries: Seq[Stmt],
    override val label: Label) extends BBlock[KaiMing](parent, entries, label)

class IRCfg[A <: MachArch](override val parent: Context[A]) extends Cfg[KaiMing, IRBBlock[A]] {
  
  private def liftToIR[A <: MachArch](ctx: Context[A]) = {
    import scalax.collection.Graph
    import scalax.collection.edge.Implicits._
    import scalax.collection.edge.LDiEdge
    import edu.psu.ist.plato.kaiming.ir.JmpStmt
    import edu.psu.ist.plato.kaiming.ir.JmpStmt
    
    val cfg = ctx.proc.cfg
    val (bbs, bbmap, nStmts) = cfg.blocks.foldLeft(
      (Vector[IRBBlock[A]](), Map[MachBBlock[A], IRBBlock[A]](), 0L)) {
        case ((bblist, map, start), bb) =>
          val builder = bb.foldLeft(IRBuilder(ctx, start, Nil)) {
            (b, inst) => ctx.proc.mach.toIRStatements(inst, b)
          }
          val irlist = builder.get
          val irbb = new IRBBlock(ctx, irlist, Label("L_" + bblist.size))
          (bblist :+ irbb, map + (bb -> irbb), builder.nextIndex)
        }
  
    object LEdgeImplicit extends scalax.collection.edge.LBase.LEdgeImplicits[Boolean]
    import LEdgeImplicit._
    cfg.graph.edges.foreach {
      x => if (!x) bbs(x.from.value).lastEntry.asInstanceOf[JmpStmt].relocate(bbs(x.to.value))  
    }
    (cfg.graph, bbmap.toList.map { case (a, b) => (b, a) }.toMap, bbs, bbs.zipWithIndex.toMap)
  }
  
  val (graph, bbmap, blocks, blockIdMap) = liftToIR(parent)
  
  def getMachBBlock(irbb: IRBBlock[A]) = bbmap.get(irbb)
}

case class IRBuilder[A <: MachArch](val ctx: Context[A], private val start: Long,
    private val content: List[Stmt]) {
    
  def get = content.reverse
  def nextIndex = start + content.size
    
  def assign(host: MachEntry[A], definedLval: Lval, usedRval: Expr) =
    IRBuilder(ctx, start, AssignStmt(nextIndex, host, definedLval, usedRval)::content)
    
  def store(host: MachEntry[A], storeTo: Expr, storedExpr: Expr) =
    IRBuilder(ctx, start, StStmt(nextIndex, host, storeTo, storedExpr)::content)
      
  def jump(host: MachEntry[A] with Terminator[A] forSome { type A <: MachArch },
      cond: Expr, target: Expr) = 
    IRBuilder(ctx, start, JmpStmt(nextIndex, host, cond, target)::content)
      
  def call(host: MachEntry[A] with Terminator[A] forSome { type A <: MachArch },
      target: Expr) =
    IRBuilder(ctx, start, CallStmt(nextIndex, host, target)::content)
      
  def load(host: MachEntry[A], definedLval: Lval, loadFrom: Expr) =
    IRBuilder(ctx, start, LdStmt(nextIndex, host, definedLval, loadFrom)::content)
      
  def select(host: MachEntry[A], definedLval: Lval, condition: Expr,
      trueValue: Expr, falseValue: Expr) =
    IRBuilder(ctx, start, SelStmt(nextIndex, host, definedLval, condition, trueValue, falseValue)::content)
      
  def ret(host: MachEntry[A], target: Expr) =
    IRBuilder(ctx, start, RetStmt(nextIndex, host, target)::content)
}

object Context {
  
  sealed trait Definition {
    
    final def get = this match {
      case Def(s) => s
      case Init => throw new NoSuchElementException
    }
    
    final def getOption = this match {
      case Def(s) => Some(s)
      case Init => None
    } 

    final def flatMap[B](f: DefStmt => Option[B]): Option[B] =
      this match {
        case Def(s) => f(s)
        case Init => None
    }
    
    final def map[B](f: DefStmt => B): Option[B] =
      this match {
        case Def(s) => Some(f(s))
        case Init => None
      }
    
  }
  case class Def(s: DefStmt) extends Definition
  case object Init extends Definition
  
  type UseDefChain = Map[Lval, Set[Definition]]
  object UseDefChain {
    def apply() = Map[Lval, Set[Definition]]()
  }
  
  private class ReachingDefinition(ctx: Context[_ <: MachArch])
      extends PathInsensitiveProblem[UseDefChain](ctx, Forward, Int.MaxValue) {
    
    // There is a more functional way to implement this, but
    // that takes too much effort which is not quite worth it
    private[this] var _UDMap = ctx.entries.map { s => (s -> UseDefChain()) }.toMap
    
    override protected def getInitialEntryState(bid: Int) = {
      val bb = ctx.cfg.blocks(bid)
      if (ctx.cfg.entryBlock == bb)
        ctx.mach.registers.map { r => (Reg(r) -> Set[Definition](Init)) }.toMap
      else
        UseDefChain()
    }
    
    override protected def confluence(dataSet: Set[UseDefChain]) = {
      dataSet.foldLeft(UseDefChain()) {
        (a, b) => (a.keySet | b.keySet).map {
            x => (x -> (a.getOrElse(x, Set()) | b.getOrElse(x, Set())))
        }.toMap
      }
    }
    
    override protected def transfer(bid: Int, in: UseDefChain) = {
      val bb = ctx.cfg.blocks(bid)
      bb.foldLeft(in) {
        (udc, entry) => {
          val stmt: Stmt = entry
          val out = stmt.usedLvals.foldLeft(udc) {
            (map, lv) => {
              val key = lv match {
                case Reg(r) if !map.contains(lv) => Reg(r.containingRegister)
                case _ => lv
              }
              val mapp = if (!map.contains(key)) (map + (key -> Set[Definition]())) else map
              _UDMap += (stmt -> (_UDMap.get(stmt).get + (lv -> mapp.get(key).get)))
              mapp
            }
          }
          val fout = stmt match {
            case ds: DefStmt => {
              val definedLval = ds.definedLval
              val defSet = Set[Definition](Def(ds))
              val tmp = out + (definedLval -> defSet)
              definedLval match {
                case Reg(mr) =>
                  mr.subsumedRegisters.foldLeft(tmp) {
                    (tmp, smr) => tmp + (Reg(smr) -> defSet)  
                  } + (Reg(mr.containingRegister) -> defSet)
                case _ => tmp
              }
            }
            case _ => out
          }
          fout
        }
      }
    }
    
    def anlayze = { val evaluate = solve; _UDMap }
  }
  
  private def useDefAnalysis[A <: MachArch](ctx: Context[A]) =
    new Context.ReachingDefinition(ctx).anlayze
    
}

final class Context[A <: MachArch] (val proc: MachProcedure[A])
    extends Procedure[Arch.KaiMing] {

  @inline def mach = proc.mach
  
  private val _tempVarPrefix = "__tmp_"
  private val _varMap = scala.collection.mutable.Map[String, Var]()

  override def label = proc.label
  override val cfg = new IRCfg(this)
  @inline override def entries = cfg.entries 
  
  override def deriveLabelForIndex(index: Long) = {
    Label("_sub_" + index.toHexString)
  }
  def getNewVar(name: String, sizeInBits: Int = mach.wordSizeInBits) = {
    if (_varMap.contains(name))
      None
    else {
      val v = Var(this, name, sizeInBits)
      _varMap += (name -> v)
      Some(v)
    }
  }
  def getNewTempVar(sizeInBits: Int) = {
    def tryUntilSuccess(number: Int): Var =
      getNewVar(_tempVarPrefix + number, sizeInBits) match {
        case Some(v) => v
        case None => tryUntilSuccess(number + 1)
      }
    tryUntilSuccess(_varMap.size)
  }
  def getNewTempVar: Var = getNewTempVar(mach.wordSizeInBits)

  lazy val useDefMap = Context.useDefAnalysis(this)
  def definitionFor(s: Stmt) = useDefMap.get(s)

  def definitionFor(s: Stmt, lv: Lval) =
    for {
      udchain <- useDefMap.get(s)
      definition <- udchain.get(lv)
    } yield definition

  private lazy val dataDependency =
    entries.foldLeft(Graph[DefStmt, DiEdge]()) {
      case (ddg, ds: DefStmt) =>
        val df = Context.Def(ds)
        ddg ++ (for {
          lv <- ds.usedLvals
          definition <- definitionFor(ds, lv).get
          dds <- definition
        } yield { new DiEdge[DefStmt](ds, dds) })
      case (ddg, _) => ddg
    }
    
  def hasCyclicDefinition(s: Stmt, lv: Lval) =
    definitionFor(s, lv) match {
      case None => false
      case Some(s) => s.exists { 
        case Context.Def(ds) => {
          val start = dataDependency.get(ds)
          import scalax.collection.GraphEdge.DiEdge
          start.findCycle.isDefined ||
            start.outerNodeTraverser.exists {
              x => dataDependency.contains(new DiEdge(x, x))
            }
        }
        case Context.Init => false
      }
    }

  def hasCyclicDefinition(s: Stmt, e: Expr): Boolean = 
    e.enumLvals.exists(hasCyclicDefinition(s, _))

  def flattenExpr(s: Stmt, e: Expr): Option[Expr] = {
    def flattenExprImpl(s: Stmt, e: Expr): Option[Expr] = {
      e match {
        case c: Const => Some(c)
        case lv: Lval => definitionFor(s, lv) match {
          case None => Exception.unreachable()
          case Some(s) => if (s.size != 1) None else s.head match {
            case Context.Def(ds) => ds match {
              case call: CallStmt => None
              case sel: SelStmt => None
              case ld: LdStmt => None 
              case assign: AssignStmt => flattenExprImpl(ds, ds.usedExpr(0))
            }
            case Context.Init => None
          }
        }
        case ce: CompoundExpr =>
          val (substitutionMap, valid) = ce.enumLvals.foldLeft((Map[Lval, Expr](), true)) {
            case ((m, v), lv) =>
              if (v) {
                flattenExprImpl(s, lv) match {
                  case None => (m, false)
                  case Some(expr) => (m + (lv -> expr), true) 
                }
              } else (m, v)
          }
          if (valid) Some(ce.substituteLvals(substitutionMap)) else None
      }
    }
    if (hasCyclicDefinition(s, e))
      None
    else
      flattenExprImpl(s, e)
  }

}
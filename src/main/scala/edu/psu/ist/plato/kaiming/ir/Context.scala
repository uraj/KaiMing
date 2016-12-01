package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.KaiMing

import edu.psu.ist.plato.kaiming.ir.dataflow.PathInsensitiveProblem
import edu.psu.ist.plato.kaiming.ir.dataflow.Forward

import edu.psu.ist.plato.kaiming.utils.Exception

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.edge.LDiEdge

class IRBBlock[A <: MachArch](override val parent: Context[A], override val entries: Seq[Stmt[A]],
    override val label: Label) extends BBlock[KaiMing](parent, entries, label) with Iterable[Stmt[A]] {
  
  override def iterator = entries.iterator
  override def firstEntry = entries.head
  
}

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
      x => if (!x) bbs(x.from.value).lastEntry.asInstanceOf[JmpStmt[A]].relocate(bbs(x.to.value))  
    }
    (cfg.graph, bbmap.toList.map { case (a, b) => (b, a) }.toMap, bbs, bbs.zipWithIndex.toMap)
  }
  
  val (graph, bbmap, blocks, blockIdMap) = liftToIR(parent)
  
  override def entries: Vector[Stmt[A]] = blocks.flatMap(_.entries)
  
  def getMachBBlock(irbb: IRBBlock[A]) = bbmap.get(irbb)
}

case class IRBuilder[A <: MachArch](val ctx: Context[A], private val start: Long,
    private val content: List[Stmt[A]]) {
    
  def get = content.reverse
  def nextIndex = start + content.size
    
  def assign(host: MachEntry[A], definedLval: Lval, usedRval: Expr) =
    IRBuilder(ctx, start, AssignStmt(nextIndex, host, definedLval, usedRval)::content)
    
  def store(host: MachEntry[A], storeTo: Expr, storedExpr: Expr) =
    IRBuilder(ctx, start, StStmt(nextIndex, host, storeTo, storedExpr)::content)
      
  def jump(host: MachEntry[A] with Terminator[A], cond: Expr, target: Expr) = 
    IRBuilder(ctx, start, JmpStmt[A](nextIndex, host, cond, target)::content)
      
  def call(host: MachEntry[A] with Terminator[A], target: Expr) =
    IRBuilder(ctx, start, CallStmt(nextIndex, host, target)::content)
      
  def load(host: MachEntry[A], definedLval: Lval, loadFrom: Expr) =
    IRBuilder(ctx, start, LdStmt(nextIndex, host, definedLval, loadFrom)::content)
      
  def select(host: MachEntry[A], definedLval: Lval, condition: Expr,
      trueValue: Expr, falseValue: Expr) =
    IRBuilder(ctx, start, SelStmt(nextIndex, host, definedLval, condition, trueValue, falseValue)::content)
      
  def ret(host: MachEntry[A], target: Expr) =
    IRBuilder(ctx, start, RetStmt(nextIndex, host, target)::content)
    
  def unsupported(host: MachEntry[A]) = IRBuilder(ctx, start, UnsupportedStmt(nextIndex, host)::content)
}

object Context {
  
  case class Def[A <: MachArch](s: DefStmt[A]) extends Definition[A]
  case object Init extends Definition[Nothing]

  sealed trait Definition[+A <: MachArch] {
    /*
    final def get: DefStmt[A] = this match {
      case Def(s) => s
      case Init => throw new NoSuchElementException
    }
    
    final def getOption = this match {
      case Def(s) => Some(s)
      case Init => None
    } 

    final def flatMap[B](f: DefStmt[A] => Option[B]): Option[B] =
      this match {
        case Def(s) => f(s)
        case Init => None
    }
    */
    final def map[B](f: DefStmt[_ <: MachArch] => B): Option[B] =
      this match {
        case Def(s) => Some(f(s))
        case Init => None
      }
    
  }
  
  type UseDefChain[A <: MachArch] = Map[Lval, Set[Definition[A]]]
  object UseDefChain {
    def apply[A <: MachArch]() = Map[Lval, Set[Definition[A]]]()
  }
  
  private class ReachingDefinition[A <: MachArch](ctx: Context[A])
      extends PathInsensitiveProblem[UseDefChain[A]](ctx, Forward, Int.MaxValue) {
    
    // There is a more functional way to implement this, but
    // that takes too much effort which is not quite worth it
    private[this] var _UDMap = ctx.entries.map { s => (s -> UseDefChain[A]()) }.toMap
    
    override protected def getInitialEntryState(bid: Int) = {
      val bb = ctx.cfg.blocks(bid)
      if (ctx.cfg.entryBlock == bb)
        ctx.mach.registers.map { r => (Reg(r) -> Set[Definition[A]](Init)) }.toMap
      else
        UseDefChain()
    }
    
    override protected def confluence(dataSet: Set[UseDefChain[A]]) = {
      dataSet.foldLeft(UseDefChain[A]()) {
        (a, b) => (a.keySet | b.keySet).map {
            x => (x -> (a.getOrElse(x, Set()) | b.getOrElse(x, Set())))
        }.toMap
      }
    }
    
    override protected def transfer(bid: Int, in: UseDefChain[A]) = {
      val bb = ctx.cfg.blocks(bid)
      bb.foldLeft(in) {
        (udc, stmt) => {
          val out = stmt.usedLvals.foldLeft(udc) {
            (map, lv) => {
              val key = lv match {
                case Reg(r) if !map.contains(lv) => Reg(r.containingRegister)
                case _ => lv
              }
              val mapp = if (!map.contains(key)) (map + (key -> Set[Definition[A]]())) else map
              _UDMap += (stmt -> (_UDMap.get(stmt).get + (lv -> mapp.get(key).get)))
              mapp
            }
          }
          val fout = stmt match {
            case ds: DefStmt[A] => {
              val definedLval = ds.definedLval
              val defSet = Set[Definition[A]](Def(ds))
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

  lazy val useDefMap = Context.useDefAnalysis[A](this)
  def definitionFor(s: Stmt[A]) = useDefMap.get(s)

  def definitionFor(s: Stmt[A], lv: Lval) =
    for {
      udchain <- useDefMap.get(s)
      definition <- udchain.get(lv)
    } yield definition

  private lazy val dataDependency =
    entries.foldLeft(Graph[DefStmt[A], DiEdge]()) {
      case (ddg, ds: DefStmt[A]) =>
        val df = Context.Def(ds)
        ddg ++ (for {
          lv <- ds.usedLvals
          definition <- definitionFor(ds, lv).get
          dds <- definition
        } yield { new DiEdge[DefStmt[A]](ds, dds) })
      case (ddg, _) => ddg
    }
    
  def hasCyclicDefinition(s: Stmt[A], lv: Lval) =
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

  def hasCyclicDefinition(s: Stmt[A], e: Expr): Boolean = 
    e.enumLvals.exists(hasCyclicDefinition(s, _))

  def flattenExpr(s: Stmt[A], e: Expr): Option[Expr] = {
    def flattenExprImpl(s: Stmt[A], e: Expr): Option[Expr] = {
      e match {
        case c: Const => Some(c)
        case lv: Lval => definitionFor(s, lv) match {
          case None => Exception.unreachable()
          case Some(s) => if (s.size != 1) None else s.head match {
            case Context.Def(ds) => ds match {
              case call: CallStmt[A] => None
              case sel: SelStmt[A] => None
              case ld: LdStmt[A] => None 
              case assign: AssignStmt[A] => flattenExprImpl(ds, ds.usedExpr(0))
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
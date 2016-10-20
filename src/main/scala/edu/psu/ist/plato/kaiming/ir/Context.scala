package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.Arch.KaiMing

import edu.psu.ist.plato.kaiming.ir.dataflow.PathInsensitiveProblem
import edu.psu.ist.plato.kaiming.ir.dataflow.Forward

import edu.psu.ist.plato.kaiming.utils.Exception

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.edge.LDiEdge


class IRCfg(override val parent : Context, override val entryBlock: IRBBlock,
    graph: Graph[IRBBlock, LDiEdge], override val hasIndirectJmp: Boolean,
    private val _bbmap: Map[IRBBlock, MachBBlock[_]],
    override val hasDanglingJump: Boolean)
    extends Cfg[KaiMing, IRBBlock](graph) {
  
  def getMachBBlock(irbb: IRBBlock) = _bbmap.get(irbb)
  
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
  
  private class ReachingDefinition(ctx: Context)
      extends PathInsensitiveProblem[UseDefChain](ctx, Forward, Int.MaxValue) {
    
    // There is a more functional way to implement this, but
    // that takes too much effort which is not quite worth it
    private[this] var _UDMap = ctx.entries.map { s => (s -> UseDefChain()) }.toMap
    
    override protected def getInitialEntryState(bb: IRBBlock) = {
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
    
    override protected def transfer(bb: IRBBlock, in: UseDefChain) = {
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
  
  private def useDefAnalysis(ctx: Context) =
    new Context.ReachingDefinition(ctx).anlayze
    
}

final class Context (val proc: MachProcedure[_ <: MachArch])
    extends Procedure[Arch.KaiMing] {

  @inline def mach = proc.mach
  
  private val _tempVarPrefix = "__tmp_"
  private val _varMap = scala.collection.mutable.Map[String, Var]()

  override def label = proc.label
  override val cfg = proc.liftCFGToIR(this)

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
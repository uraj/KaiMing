package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming._
import edu.psu.ist.plato.kaiming.ir.dataflow.PathInsensitiveProblem
import edu.psu.ist.plato.kaiming.ir.dataflow.Forward

object Context {
  
  sealed trait Definition
  case class Def(s: DefStmt) extends Definition
  case object Init extends Definition
  
  type UseDefChain = Map[Lval, Set[Definition]]
  object UseDefChain {
    def apply() = Map[Lval, Set[Definition]]()
  }
  
  private class ReachingDefinition(ctx: Context)
      extends PathInsensitiveProblem[UseDefChain](ctx, Forward, Int.MaxValue) {
    
    private type BB = BBlock[Arch.KaiMing]
    
    // There is a way to implement this in a more functional way, but
    // that takes too much effort which is not quite worth it
    private var _UDMap = ctx.entries.map { s => (s -> UseDefChain()) }.toMap
    
    override protected def getInitialEntryState(bb: BB) = {
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
    
    override protected def transfer(bb: BB, in: UseDefChain) = {
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

class Context (val proc: MachProcedure[_ <: MachArch])
    extends Procedure[Arch.KaiMing] {

  val mach = proc.mach
  
  private val _tempVarPrefix = "__tmp_"
  private val _varMap = scala.collection.mutable.Map[String, Var]()
  
  override val label = proc.label
  val (cfg, mapToMachBBlock) = proc.liftCFGToIR(this)

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
       
  final lazy val useDefMap = Context.useDefAnalysis(this)
  final def definitionFor(s: Stmt) = useDefMap.get(s)

  final def definitionFor(s: Stmt, lv: Lval) =
    for {
      udchain <- useDefMap.get(s)
      definition <- udchain.get(lv)
    } yield definition

    
}
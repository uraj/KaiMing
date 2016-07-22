package edu.psu.ist.plato.kaiming.ir

import edu.psu.ist.plato.kaiming._

class Context private (val proc: MachProcedure[_])
    extends Procedure[Arch.KaiMing] {

  override val label = proc.label
  override val cfg = proc.liftCFGToIR(this)
  
  val mach = proc.mach
  
  override def deriveLabelForIndex(index: Long) = {
    Label("_sub_" + index.toHexString)
  }
  
  private val _varMap = scala.collection.mutable.Map[String, Var]()
  private val _tempVarPrefix = "__tmp_"
  def getNewVar(name: String, sizeInBits: Int = mach.wordSizeInBits) = {
    if (_varMap.contains(name))
      None
    else {
      val v = Var(this, name, sizeInBits)
      _varMap += (name -> v)
      Some(v)
    }
  }
  
  def getNewTempVar = getNewVar(_tempVarPrefix + _varMap.size)
  def getNewTempVar(sizeInBits: Int) =
    getNewVar(_tempVarPrefix + _varMap.size, sizeInBits)
  
}
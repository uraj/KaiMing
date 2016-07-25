package edu.psu.ist.plato.kaiming

package object ir {
  
  import edu.psu.ist.plato.kaiming.Arch.KaiMing
  import scala.language.implicitConversions
  implicit def toIRStmt(e: Entry[KaiMing]) = e.asInstanceOf[Stmt]
  
  type IRBBlock = BBlock[KaiMing]
  type IRCfg = Cfg[KaiMing, IRBBlock]
  
}
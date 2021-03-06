package io.github.uraj.kaiming.ir

import java.io.PrintStream
import java.io.OutputStream

import io.github.uraj.kaiming.BBlock
import io.github.uraj.kaiming.Arch

object IRPrinter {
  
  val out = new IRPrinter(Console.out)
  val err = new IRPrinter(Console.err)
  
  private val endOfStmt = ';' 
  
}

final class IRPrinter(ps: OutputStream) extends PrintStream(ps) {
  
  private def EOS = print(IRPrinter.endOfStmt)
  
  def printConst(c: Const) {
    if (c.value < 0)
      print("-0x%x".format(-c.value))
    else
      print("0x%x".format(c.value))
  }
  
  def printReg(r: Reg) {
    print('%')
    print(r.name)
  }
  
  def printVar(v: Var) {
    print("@(")
    print(v.name)
    print(":")
    print(v.sizeInBits)
    print(')')
  }
  
  def printFlg(f: Flg) {
    print('%')
    print(f.name)
  }
  
  def printLval(lv: Lval) {
    lv match {
      case r: Reg => printReg(r)
      case v: Var => printVar(v)
      case f: Flg => printFlg(f)
    }
  }
  
  def printBExpr(e: BExpr) {
    e.leftSub match {
      case be: BExpr =>
        print('(')
        printBExpr(be)
        print(')')
      case _ => printExpr(e.leftSub)
    }
    print(' ')
    e match {
      case _: Add    => print('+')
      case _: And    => print('&')
      case _: Concat => print(":+")
      case _: SDiv   => print("/-")
      case _: UDiv   => print("/+")
      case _: Mul    => print('*')
      case _: Or     => print('|')
      case _: Sar    => print(">>>")
      case _: Shl    => print("<<")
      case _: Shr    => print(">>")
      case _: Ror    => print("><")
      case _: Sub    => print('-')
      case _: Xor    => print('^')
      case _: SExt   => print("sext")
      case _: UExt   => print("uext")
      case _: High   => print("|<")
      case _: Low    => print("|>")
      case _: BSwap  => print("<>")
    }
    print(' ')
    e.rightSub match {
      case be: BExpr =>
        print('(')
        printBExpr(be)
        print(')')
      case _ => printExpr(e.rightSub)
    }
  }
  
  def printUExpr(e: UExpr) {
    e match {
      case _: Not => print("~")
      case _: Neg => print("!")
      case _ =>
    }
    e.sub match {
      case be: BExpr =>
        print('(')
        printBExpr(be)
        print(')')
      case _ => printExpr(e.sub)
    }
    e match {
      case _: Carry => print("@!")
      case _: Overflow => print("@^")
      case _: Zero => print("@*")
      case _: Negative => print("@-")
      case _ =>
    }
  }
  
  def printExpr(e: Expr) {
    e match {
      case c: Const  => printConst(c)
      case lv: Lval  => printLval(lv)
      case be: BExpr => printBExpr(be)
      case ue: UExpr => printUExpr(ue)
    }
  }
  
  def printAssignStmt(s: AssignStmt[_ <: Arch]) {
    printLval(s.definedLval)
    print(" = ")
    printExpr(s.usedRval)
  }
  
  def printCallStmt(s: CallStmt[_ <: Arch]) {
    print("call ")
    printExpr(s.target)
  }
  
  def printJmpStmt(s: JmpStmt[_ <: Arch]) {
    print("jmp")
    if (s.isConditional) {
      print('[')
      printExpr(s.cond)
      print(']')
    }
    print(" ")
    s.relocatedTarget match {
      case Some(bb) => print(bb.label.name)
      case None => printExpr(s.target)
    }
  }
  
  def printLdStmt(s: LdStmt[_ <: Arch]) {
    printLval(s.definedLval)
    print(" <- [ ")
    printExpr(s.loadFrom)
    print(" ]")
  }
  
  def printStStmt(s: StStmt[_ <: Arch]) {
    printExpr(s.storedExpr)
    print(" -> [ ")
    printExpr(s.storeTo)
    print(" ]")
  }
  
  def printSelStmt(s: SelStmt[_ <: Arch]) {
    printLval(s.definedLval)
    print(" = ")
    printExpr(s.condition)
    print(" ? ")
    printExpr(s.trueValue)
    print(" : ")
    printExpr(s.falseValue)
  }
  
  def printRetStmt(s: RetStmt[_ <: Arch]) {
    print("ret")
  }
  
  def printStmt[A <: Arch](s: Stmt[A]) {
    s match {
      case s: AssignStmt[A] => printAssignStmt(s)
      case s: CallStmt[A] => printCallStmt(s)
      case s: JmpStmt[A] => printJmpStmt(s)
      case s: LdStmt[A] => printLdStmt(s)
      case s: StStmt[A] => printStStmt(s)
      case s: SelStmt[A] => printSelStmt(s)
      case s: RetStmt[A] => printRetStmt(s)
      case s: NopStmt[A] => print("Nop")
      case s: UnsupportedStmt[A] =>
        print(s"Unsupported ${s.host.index.toHexString}")
    }
    EOS
  }
  
  def printBasicBlock[A <: Arch](bb: IRBBlock[A]) {
    println(bb.label)
    bb.foreach { s => { printStmt(s); println() } }
  }
  
  def printIndentedBasicBlock[A <: Arch](bb: IRBBlock[A]) {
    println(bb.label)
    bb.foreach { s => { print("\t"); printStmt(s); println() } }
  }
  
  def printContext(ctx: Context[_ <: Arch]) {
    print(ctx.name);
    println(" {")
    ctx.cfg.blocks.foreach { bb => printIndentedBasicBlock(bb) }
    println('}')
  }
  
  def printContextWithUDInfo[A <: Arch](ctx: Context[A]) {
    print(ctx.name)
    println(" {")
    for (bb <- ctx.cfg) {
      println(bb.label)
      for (s <- bb) {
        print('\t')
        printStmt(s)
        print("\t#")
        print(s.host.index.toHexString)
        print(' ')
        println(s.index)
        ctx.definitionFor(s) match {
          case None => printStmt(s); println;
          case _ =>
        }
        for ((lv, defs) <- ctx.definitionFor(s).get) {
          print("\t\t# ")
          printLval(lv)
          print(" -- ")
          for (definition <- defs) {
            definition match {
              case Context.Def(d) => print(d.index)
              case Context.Init => print(Context.Init)
            }
            print(',')
          }
          println()
        }
      }
    }
    println('}')
  }
  
}
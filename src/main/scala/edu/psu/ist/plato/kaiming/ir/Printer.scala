package edu.psu.ist.plato.kaiming.ir

import java.io.PrintStream
import java.io.OutputStream

import edu.psu.ist.plato.kaiming.BBlock
import edu.psu.ist.plato.kaiming.Arch.KaiMing

object Printer {
  
  val out = new Printer(Console.out)
  val err = new Printer(Console.err)
  
  private val endOfStmt = ';' 
  
}

final class Printer(ps: OutputStream) extends PrintStream(ps) {
  
  private def EOS = print(Printer.endOfStmt)
  
  def printConst(c: Const) {
    if (c.value < 0)
      print("-0x%x".format(-c.value))
    else
      print("0x%x".format(c.value))
  }
  
  def printReg(r: Reg) {
    print('%')
    print(r.mreg.name)
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
    print(f.mflag.name)
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
      case _: Concat => print(':')
      case _: Div    => print('/')
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
      case _: BSwap => print("<>")
      case _: High => print("high")
      case _: Low => print("low")
      case _: Not => print("~")
    }
    e.sub match {
      case be: BExpr =>
        print('(')
        printBExpr(be)
        print(')')
      case _ => printExpr(e.sub)
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
  
  def printAssignStmt(s: AssignStmt) {
    printLval(s.definedLval)
    if (s.isPartial) {
      print('<')
      print(s.range._1)
      print(',')
      print(s.range._2)
      print('>')
    }
    print(" = ")
    printExpr(s.usedRval)
    EOS
  }
  
  def printCallStmt(s: CallStmt) {
    print("call ")
    printExpr(s.target)
    EOS
  }
  
  def printJmpStmt(s: JmpStmt) {
    print("jmp")
    if (s.isConditional) {
      print('[')
      print(s.dependentFlags.map { f => f.name }.mkString(","))
      print(']')
    }
    print(" ")
    s.relocatedTarget match {
      case Some(bb) => print(bb.label.name)
      case None => printExpr(s.target)
    }
    EOS
  }
  
  def printLdStmt(s: LdStmt) {
    printLval(s.definedLval)
    print(" <- [ ")
    printExpr(s.loadFrom)
    print(" ]")
    EOS
  }
  
  def printStStmt(s: StStmt) {
    printExpr(s.storedExpr)
    print(" -> [ ")
    printExpr(s.storeTo)
    print(" ]")
    EOS
  }
  
  def printSelStmt(s: SelStmt) {
    printLval(s.definedLval)
    print(" = ")
    printExpr(s.condition)
    print(" ? ")
    printExpr(s.trueValue)
    print(" : ")
    printExpr(s.falseValue)
    EOS
  }
  
  def printSetFlgStmt(s: SetFlgStmt) {
    printFlg(s.definedLval)
    print(" <~ ")
    print(s.extractor.entryName)
    print(' ')
    printExpr(s.usedRval)
    EOS
  }
  
  def printRetStmt(s: RetStmt) {
    print("ret")
    EOS
  }
  
  def printStmt(s: Stmt) {
    s match {
      case s: AssignStmt => printAssignStmt(s)
      case s: CallStmt => printCallStmt(s)
      case s: JmpStmt => printJmpStmt(s)
      case s: LdStmt => printLdStmt(s)
      case s: StStmt => printStStmt(s)
      case s: SelStmt => printSelStmt(s)
      case s: SetFlgStmt => printSetFlgStmt(s)
      case s: RetStmt => printRetStmt(s)
    }
  }
  
  def printBasicBlock(bb: BBlock[KaiMing]) {
    println(bb.label)
    bb.foreach { s => { printStmt(s); println() } }
  }
  
  def printIndentedBasicBlock(bb: BBlock[KaiMing]) {
    println(bb.label)
    bb.foreach { s => { print("\t"); printStmt(s); println() } }
  }
  
  def printContext(ctx: Context) {
    print(ctx.name);
    println(" {")
    ctx.cfg.blocks.foreach { bb => printIndentedBasicBlock(bb) }
    println('}')
  }
  
  def printContextWithUDInfo(ctx: Context) {
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
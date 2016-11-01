package edu.psu.ist.plato.kaiming.test.aarch64

import scala.io.Source

import java.io.File
import java.io.ByteArrayOutputStream

import org.scalatest.FunSuite

import edu.psu.ist.plato.kaiming.aarch64.Function
import edu.psu.ist.plato.kaiming.aarch64.AArch64Parser

import edu.psu.ist.plato.kaiming.ir.Context
import edu.psu.ist.plato.kaiming.ir.Loop

class TestAArch64 extends FunSuite {
  
  var testFuncs = Vector[Function]()

  test("Testing AArch64 parser and CFG construction [OK]") {
    import edu.psu.ist.plato.kaiming.aarch64.AArch64Printer

    for (no <- List(1, 2)) {
      val name = "/test/aarch64/test-" + "%02d".format(no) + ".s"
      val file = new File(getClass.getResource(name).toURI) 
      println(file.getName)
      val source = Source.fromFile(file, "UTF-8")
      val input = source.mkString
      print("Parsing " + file.getName + " : ")
      val result: (Option[List[Function]], String) = 
        AArch64Parser.parseAll(AArch64Parser.binaryunit, input) match {
          case AArch64Parser.Success(value, _) => (Some(value), "")
          case AArch64Parser.NoSuccess(msg, next) =>
            (None, msg + " " +  next.offset + " " + next.pos)
        }
      result match {
        case (None, msg) =>
          println("Fail (" + msg + ")")
        case (Some(funcs), _) =>
          println("OK")
          val baos = new ByteArrayOutputStream
          val printer = new AArch64Printer(baos)
          for (func <- funcs) {
            printer.printCFG(func.cfg)
          }
          testFuncs ++= funcs
          source.close
          printer.close
          println(baos.toString)
      }
    }
  }
  
  test("Testing AArch64 IR lifting and UD analysis [OK]") {
    import edu.psu.ist.plato.kaiming.ir.IRPrinter
    for (func <- testFuncs) {
      val ctx = new Context(func)
      IRPrinter.out.printContextWithUDInfo(ctx)
    }
    
  }
  
  test("Test large-scale parsing and IR lifting [OK]") {
    val name = "/test/aarch64/test-03.s"
    val file = new File(getClass.getResource(name).toURI) 
    val funcs = AArch64Parser.parseFile(file)
    val ctxes = funcs.map(new Context(_))
    var flaCount = 0
    val threshold = .8
    for (c <- ctxes) {
      val cfg = c.cfg
      val bloops = Loop.detectOuterLoops(cfg)
      if (cfg.isConnected) {
        for (bloop <- bloops) {
          if (bloop.body.size >= cfg.size * threshold) {
            flaCount += 1
            println(c.proc.name)
          }
        }
      }
      else {
        for (bloop <- bloops) {
          val subcfg = cfg.belongingComponent(bloop.header).get
          if (bloop.body.size >= subcfg.nodes.size * threshold) {
            flaCount += 1
            println(c.proc.name, c.proc.index.toHexString)
          }
        }
      }
    }
    println(s"${funcs.length} functions successfully parsed, $flaCount flattened")
  }

}
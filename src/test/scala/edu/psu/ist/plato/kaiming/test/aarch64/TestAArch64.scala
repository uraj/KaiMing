package edu.psu.ist.plato.kaiming.test.aarch64

import scala.io.Source

import java.io.File
import java.io.ByteArrayOutputStream

import org.scalatest.FunSuite

import edu.psu.ist.plato.kaiming.aarch64.Function
import edu.psu.ist.plato.kaiming.aarch64.AArch64Parser
import edu.psu.ist.plato.kaiming.aarch64.AArch64Printer

import edu.psu.ist.plato.kaiming.ir.Context
import edu.psu.ist.plato.kaiming.ir.Loop

class TestAArch64 extends FunSuite {
  
  var testFuncs = Vector[Function]()

  test("Testing AArch64 parser and CFG construction [OK]") {
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
          testFuncs ++= funcs
          source.close
      }
    }
  }
  
  test("Testing AArch64 IR lifting and UD analysis [OK]") {
    for (func <- testFuncs) {
      val ctx = new Context(func)
      ctx.useDefMap
    }
    
  }
  
  test("Test large-scale parsing and IR lifting [OK]") {
    val name = "/test/aarch64/test-05.s"
    val file = new File(getClass.getResource(name).toURI) 
    val funcs = AArch64Parser.parseFile(file)
    val ctxes = funcs.map { x => new Context(x._1) }
    var flaCount = 0
    val threshold = .8
    for (c <- ctxes) {
      val cfg = c.cfg
      val bloops = Loop.detectOuterLoops(cfg)
      flaCount += (
          if (cfg.isConnected) {
            bloops.count { x => x.body.size > 2 && x.body.size >= cfg.size * threshold }
          } else {
            bloops.count { x => 
            val subcfg = cfg.belongingComponent(x.header).get
            x.body.size > 2 && x.body.size >= subcfg.nodes.size * threshold
          }
        })
    }
    println(s"${funcs.length} functions successfully parsed, $flaCount flattened")
  }

}

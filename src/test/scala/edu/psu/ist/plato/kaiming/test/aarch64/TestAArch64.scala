package edu.psu.ist.plato.kaiming.test.aarch64

import scala.io.Source

import java.io.File
import java.io.ByteArrayOutputStream

import org.scalatest.FunSuite
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time.SpanSugar._

import edu.psu.ist.plato.kaiming.aarch64.Function
import edu.psu.ist.plato.kaiming.aarch64.AArch64Parser

import edu.psu.ist.plato.kaiming.ir.Context

class TestAArch64 extends FunSuite with TimeLimitedTests {
  
  override val timeLimit = 5000.millis
  
  var testFuncs = Vector[Function]()
  
  test("Testing AArch64 parser and CFG construction") {
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
  
  test("Testing AArch64 IR lifting and UD analysis") {
    import edu.psu.ist.plato.kaiming.ir.IRPrinter
    for (func <- testFuncs) {
      val ctx = new Context(func)
      IRPrinter.out.printContextWithUDInfo(ctx)
    }
    
  }
  
  test("Test large-scale parsing") {
    val name = "/test/aarch64/test-03.s"
    val file = new File(getClass.getResource(name).toURI) 
    val funcs = AArch64Parser.parseFile(file)
    println(funcs.length + " functions successfully parsed")
  }

}
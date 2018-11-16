package io.github.uraj.kaiming.test.arm

import scala.io.Source

import java.io.File
import java.io.ByteArrayOutputStream

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.scalatest.time.SpanSugar._
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.junit.JUnitRunner

import io.github.uraj.kaiming.Cfg.Loop
import io.github.uraj.kaiming.ir.Context
import io.github.uraj.kaiming.ir.Var
import io.github.uraj.kaiming.arm.ARM
import io.github.uraj.kaiming.arm.Function
import io.github.uraj.kaiming.arm.ARMParser

class TestARM extends FunSuite with BeforeAndAfter {
  
  var total = 0
  var failure = 0

  var testFuncs = Vector[Function]()
  
  test("Testing ARM parser and CFG construction") {
    import io.github.uraj.kaiming.arm.ARMPrinter
    
    val testdir = new File(getClass.getResource("/test/arm").toURI)
    val testfiles = 
      testdir.listFiles.filter { x => x.isFile && !x.isHidden }.sortWith { _.getName < _.getName }
    total = testfiles.size
    
    if (!testdir.isDirectory)
      assert(false)

    for (file <- testfiles) {
      println(file.getName)
      val source = Source.fromFile(file, "UTF-8")
      val input = source.mkString
      print("Parsing " + file.getName + " : ")
      val result: (Option[Seq[Function]], String) = 
        ARMParser.binaryunit.parse(input) match {
          case fastparse.all.Parsed.Success(value, _) => (Some(value), "")
          case f: fastparse.all.Parsed.Failure => (None, f.msg)
        }
      result match {
        case (None, msg) =>
          failure += 1
          println("Fail (" + msg + ")")
        case (Some(funcs), _) =>
          println("OK")
          val baos = new ByteArrayOutputStream
          val printer = new ARMPrinter(baos)
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
  
  var ctxList = Vector[Context[ARM]]()
  test("Testing ARM IR lifting and UD analysis") {
    import io.github.uraj.kaiming.ir.IRPrinter

    failure = testFuncs.length
    for (func <- testFuncs) {
      val ctx = new Context(func)
      IRPrinter.out.printContextWithUDInfo(ctx)
      failure -= 1
      ctxList = ctxList :+ ctx
    }
    
  }

  test("Testing ARM loop detection") {
    for (ctx <- ctxList) {
      val loops = Loop.detectLoops(ctx.cfg)
      println(loops.size + " loops detected in " + ctx.label.name)
      for (l <- loops)
        println(l)
     }
  }
  
  after {
    println((total - failure) + "/" + total + " parsing tests passed.")
  }
    
}

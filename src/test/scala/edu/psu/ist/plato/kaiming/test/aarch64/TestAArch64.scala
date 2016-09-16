package edu.psu.ist.plato.kaiming.test.aarch64

import scala.io.Source

import java.io.File
import java.io.ByteArrayOutputStream

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.scalatest.junit.JUnitRunner

import edu.psu.ist.plato.kaiming.aarch64.Function
import edu.psu.ist.plato.kaiming.aarch64.AArch64Parser

import edu.psu.ist.plato.kaiming.ir.Context

class TestAArch64 extends FunSuite with BeforeAndAfter {
  
  var testdir: File = null
  var testfiles: Array[File] = null
  var total = 0
  var failure = 0
  
  before {
    testdir = new File(getClass.getResource("/test/aarch64").toURI)
    testfiles = testdir.listFiles.filter { x => x.isFile && !x.isHidden}
    testfiles = testfiles.sortWith {(x, y)=> x.getName < y.getName}
    total = testfiles.size
  }
  
  var testFuncs = Vector[Function]()
  
  test("Testing AArch64 parser and CFG construction") {
    import edu.psu.ist.plato.kaiming.aarch64.AArch64Printer
    
    if (!testdir.isDirectory)
      assert(false)

    for (file <- testfiles) {
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
          failure += 1
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
    
    failure = 2
    for (func <- testFuncs) {
      val ctx = new Context(func)
      IRPrinter.out.printContextWithUDInfo(ctx)
      failure -= 1
    }
    
  }
  
  after {
    println((total - failure) + "/" + total + " parsing tests passed.")
  }
    
}
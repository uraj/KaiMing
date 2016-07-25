package edu.psu.ist.plato.kaiming.test.arm

import scala.io.Source

import java.io.File
import java.io.ByteArrayOutputStream

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.scalatest.junit.JUnitRunner

import org.junit.runner.RunWith

import edu.psu.ist.plato.kaiming.arm.Function
import edu.psu.ist.plato.kaiming.arm.ARMParser

import edu.psu.ist.plato.kaiming.ir.Context

@RunWith(classOf[JUnitRunner])
class Test extends FunSuite with BeforeAndAfter {
  
  var testdir: File = null
  var testfiles: Array[File] = null
  var total = 0
  var failure = 0
  
  before {
    testdir = new File(getClass.getResource("/test/arm").toURI())
    testfiles = testdir.listFiles().filter { x => x.isFile() && !x.isHidden()}
    testfiles = testfiles.sortWith {(x, y)=> x.getName < y.getName}
    total = testfiles.size
  }
  
  var testFuncs = Vector[Function]()
  
  test("Testing ARM parser and CFG construction") {
    import edu.psu.ist.plato.kaiming.arm.Printer
    
    if (!testdir.isDirectory())
      assert(false)

    for (file <- testfiles) {
      println(file.getName)
      val source = Source.fromFile(file, "UTF-8")
      val input = source.mkString
      print("Parsing " + file.getName() + " : ")
      val result: (Option[List[Function]], String) = 
        ARMParser.parseAll(ARMParser.binaryunit, input) match {
          case ARMParser.Success(value, _) => (Some(value), "")
          case ARMParser.NoSuccess(msg, next) =>
            (None, msg + " " +  next.offset + " " + next.pos)
        }
      result match {
        case (None, msg) =>
          failure += 1
          println("Fail (" + msg + ")")
        case (Some(funcs), _) =>
          println("OK")
          val baos = new ByteArrayOutputStream()
          val printer = new Printer(baos)
          for (func <- funcs) {
            printer.printCFG(func.cfg)
          }
          testFuncs ++= funcs
          source.close()
          printer.close()
          println(baos.toString())
      }
    }
  }
  
  test("Testing ARM IR lifting and UD analysis") {
    import edu.psu.ist.plato.kaiming.ir.Printer
    
    failure = testFuncs.length
    for (func <- testFuncs) {
      val ctx = new Context(func)
      Printer.out.printContextWithUDInfo(ctx)
      failure -= 1
    }
    
  }
  
  after {
    println((total - failure) + "/" + total + " parsing tests passed.")
  }
    
}
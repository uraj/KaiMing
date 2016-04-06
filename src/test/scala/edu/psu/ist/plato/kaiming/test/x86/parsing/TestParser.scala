package edu.psu.ist.plato.kaiming.test.x86.parsing


import scala.io.Source

import java.io.File
import java.io.ByteArrayOutputStream

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import edu.psu.ist.plato.kaiming.x86.parsing.GASParser
import edu.psu.ist.plato.kaiming.x86.Function
import edu.psu.ist.plato.kaiming.x86.Printer

@RunWith(classOf[JUnitRunner])
class TestParser extends FunSuite with BeforeAndAfter {
  
  var testdir : File = null
  var testfiles : Array[File] = null
  var total = 0
  var failure = 0
  
  before {
    testdir = new File(getClass.getResource("/TestParser/").toURI())
    testfiles = testdir.listFiles().filter { x => x.isFile() && !x.isHidden()}
    testfiles = testfiles.sortWith {(x, y)=> x.getName < y.getName}
    total = testfiles.size
  }
  
  test("Test edu.psu.ist.plato.kaiming.x86.parsing.Parser") {
    
    if (!testdir.isDirectory())
      assert(false)

    for (file <- testfiles) {
      println(file.getName)
      val source = Source.fromFile(file, "UTF-8")
      val input = source.mkString
      print("Parsing " + file.getName() + ": ")
      val result : (Option[List[Function]], String) = GASParser.parseAll(GASParser.binaryunit, input) match {
        case GASParser.Success(value, _) => (Some(value), "")
        case GASParser.NoSuccess(msg, next) => (None, msg)
      }
      result match {
        case (None, msg) =>
          failure += 1
          println("Fail (" + msg + ")")
        case (Some(funcs), _) =>
          println("OK")
          val baos = new ByteArrayOutputStream()
          val printer = new Printer(baos, false)
          for (func <- funcs) {
            printer.printCFG(func.cfg())
          }
          source.close()
          printer.close()
          println(baos.toString())
      }
    }
  }
  
  after {
    println((total - failure) + "/" + total + " tests passed.")
  }
    
}

package edu.psu.ist.plato.kaiming.test.x86.ir.typing

import java.io.File
import java.io.FileInputStream

import scala.io.Source

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.scalatest.junit.JUnitRunner

import edu.psu.ist.plato.kaiming.x86.ir._
import edu.psu.ist.plato.kaiming.x86.ir.typing._
import edu.psu.ist.plato.kaiming.elf.Elf
import edu.psu.ist.plato.kaiming.x86.parsing.GASParser

import org.junit.runner.RunWith

import scodec.bits._

@RunWith(classOf[JUnitRunner])
class TestConstraintSolving extends FunSuite with BeforeAndAfter {
  var elf : Elf = null;
  var ctx : Context = null; 
  
  before {
    val file = new File(getClass.getResource("/TestTI/fact").getFile)
    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)
    in.read(bytes)
    in.close()
    elf = new Elf(ByteVector(bytes))
    val asm = Source.fromFile(getClass.getResource("/TestTI/fact.s").toURI())
    ctx = new Context(GASParser.parseBinaryUnit(asm.mkString).head)
    asm.close()
    AssemblyUnit.UDAnalysis(ctx)
  }
  
  test("Testing type inference") {
    val solver = new TypeInferer(elf)
    Printer.out.printContextWithUDInfo(ctx)
    println(solver.infer(ctx))
  }
}
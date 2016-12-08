package edu.psu.ist.plato.kaiming.test.aarch64

import scala.io.Source

import java.io.{File, ByteArrayOutputStream}

import org.scalatest.FunSuite

import edu.psu.ist.plato.kaiming.aarch64.{Function, AArch64Parser => Parser}

import edu.psu.ist.plato.kaiming.Arch
import edu.psu.ist.plato.kaiming.Cfg
import edu.psu.ist.plato.kaiming.Cfg.Loop
import edu.psu.ist.plato.kaiming.ir.Context

class TestAArch64 extends FunSuite {
  
  def diameter[A <: Arch](loop: Loop[A], cfg: Cfg[A]): Int = {
    val graph = cfg.graph
    loop.body.foldLeft(0) {
      (max, x) => Math.max(max, graph.get(loop.header).shortestPathTo(graph.get(x)).get.weight.toInt)
    }
  }
  
  def log2(x: Int) =
    31 - ((0 until 32).find { y => ((x << y) & 0x80000000) != 0}).getOrElse(0)
  
  test("Test large-scale parsing and IR lifting [OK]") {
    val name = "/test/aarch64/test-04.s"
    val file = new File(getClass.getResource(name).toURI)
    var funCount = 0
    var flaCount = 0
    val threshold = .8
    for ((f, c) <- Parser.parseFile(file) if f.cfg.size > 20) {
      funCount += 1
      val bloops = Loop.detectLoops(f.cfg, true)
      var dia = 0
      var body = 0
      val hit = bloops.exists { loop =>
        dia = diameter(loop, f.cfg)
        body = loop.body.size
        val ret = body >= 20 && dia <= 2 + log2(body)
        ret
      }
      if (hit) {
        println(s"${f.index.toHexString} flattened, $dia / $body")
        flaCount += 1
      }
    }
    println(s"${funCount} functions successfully parsed, $flaCount flattened")
  }

}

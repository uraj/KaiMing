package io.github.uraj.kaiming.test.aarch64

import scala.io.Source

import java.io.{File, ByteArrayOutputStream}

import org.scalatest.FunSuite

import io.github.uraj.kaiming.aarch64.{Function, AArch64Parser => Parser}

import io.github.uraj.kaiming.Arch
import io.github.uraj.kaiming.Cfg
import io.github.uraj.kaiming.Cfg.Loop

class TestAArch64 extends FunSuite {
  
  def diameter[A <: Arch](loop: Loop[A], cfg: Cfg[A]): Int = {
    val graph = cfg.graph
    loop.body.foldLeft(0) {
      (max, x) => Math.max(max, graph.get(loop.header).shortestPathTo(graph.get(x)).get.weight.toInt)
    }
  }
  
  def log2(x: Int) =
    31 - ((0 until 32).find { y => ((x << y) & 0x80000000) != 0}).getOrElse(0)
  
  test("Test simple parsing and IR lifting [OK]") {
    val name = "/test/aarch64/test-02.s"
    val file = new File(getClass.getResource(name).toURI)
    for ((f, c) <- Parser.parseFile(file)) {}
  }

  test("Test large-scale parsing, IR lifting, and loop detection [OK]") {
    val name = "/test/aarch64/test-02.s"
    val file = new File(getClass.getResource(name).toURI)
    for ((f, c) <- Parser.parseFile(file)) {
      Loop.detectLoops(f.cfg, true)
    }
  }

}

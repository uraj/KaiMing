package edu.psu.ist.plato.kaiming.test.aarch64

import scala.io.Source

import java.io.{File, ByteArrayOutputStream}

import org.scalatest.FunSuite

import edu.psu.ist.plato.kaiming.aarch64.{Function, AArch64Parser => Parser}

import edu.psu.ist.plato.kaiming.Cfg.Loop
import edu.psu.ist.plato.kaiming.ir.Context

class TestAArch64 extends FunSuite {
  
  test("Test large-scale parsing and IR lifting [OK]") {
    val name = "/test/aarch64/test-03.s"
    val file = new File(getClass.getResource(name).toURI)
    var funCount = 0
    var flaCount = 0
    val threshold = .8
    for ((f, c) <- Parser.parseFile(file)) {
      funCount += 1
      val bloops = Loop.detectLoops(f.cfg, true)
      var flaCountRound = 0
      val connected = f.cfg.isConnected
      flaCountRound += (
        if (connected) {
          bloops.count { x => x.body.size > 10 && x.body.size >= f.cfg.size * threshold }
        } else {
          bloops.count {
            x =>
              x.body.size > 10 && x.body.size >= f.cfg.graph.nodes.size * threshold
          }
        })
      if (flaCountRound > 0)
        println(s"${f.index.toHexString} flattened")
      flaCount += flaCountRound
    }
    println(s"${funCount} functions successfully parsed, $flaCount flattened")
  }

}

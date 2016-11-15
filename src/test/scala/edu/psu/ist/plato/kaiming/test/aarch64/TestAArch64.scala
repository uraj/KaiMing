package edu.psu.ist.plato.kaiming.test.aarch64

import scala.io.Source

import java.io.{File, ByteArrayOutputStream}

import org.scalatest.FunSuite

import edu.psu.ist.plato.kaiming.aarch64.{Function, AArch64Parser, AArch64Printer}

import edu.psu.ist.plato.kaiming.ir.{Context, Loop}

class TestAArch64 extends FunSuite {
  
  test("Test large-scale parsing and IR lifting [OK]") {
    val name = "/test/aarch64/test-03.s"
    val file = new File(getClass.getResource(name).toURI)
    var funCount = 0
    var flaCount = 0
    val threshold = .8
    for (f <- AArch64Parser.parseFile(file)) {
      val time1 = System.currentTimeMillis
      funCount += 1
      val c = new Context(f._1)
      val cfg = c.cfg
      val bloops = Loop.detectLoops(cfg, true)
      var flaCountRound = 0
      val connected = cfg.isConnected
      flaCountRound += (
        if (connected) {
          bloops.count { x => x.body.size > 2 && x.body.size >= cfg.size * threshold }
        } else {
          bloops.count {
            x =>
              x.body.size > 2 && x.body.size >= x.component.nodes.size * threshold
          }
        })
      val time2 = System.currentTimeMillis
      if (flaCountRound > 0)
        println(s"${f._1.index.toHexString} flattened")
      flaCount += flaCountRound
      println(s"$funCount function processed in ${time2 - time1}ms")
    }
    println(s"${funCount} functions successfully parsed, $flaCount flattened")
  }

}

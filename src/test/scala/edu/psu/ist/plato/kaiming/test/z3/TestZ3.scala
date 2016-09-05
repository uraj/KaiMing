package edu.psu.ist.plato.kaiming.test.z3

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

import com.microsoft.z3.{Context => Z3Context}
import com.microsoft.z3.{BitVecExpr => Z3BVExpr}
import com.microsoft.z3.{BitVecNum => Z3BVNum}

import edu.psu.ist.plato.kaiming.ir._

class TestZ3 extends FunSuite with BeforeAndAfter {

  before {
    Symbolic.init
  }
  
  test("Simple simplify") {
    val ctx = new Z3Context
    val x = ctx.mkBVConst("x", 32);
    val y = ctx.mkBVConst("y", 32);
    val z = ctx.mkBVConst("z", 32);

    println(Symbolic(Const(1, 32) + Const(2, 32)).simplify())
  }

}
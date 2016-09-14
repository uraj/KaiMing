package edu.psu.ist.plato.kaiming.test.ir

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
  
  test("Simple substitution") {
    
    implicit def toConst(i: Int) = Const(i, 32)
    
    val vx = Var(null, "x", 32)
    val vy = Var(null, "y", 32)
    val vz = Var(null, "z", 32)
    val expr = (1 + vx) * vy - (vz * 31 - 2)
    
    assert(expr.substitute(1 + vx, vz) == vz * vy - (vz * 31 - 2),
        expr.substitute(1 + vx, vz))
    
    assert(expr.substituteLvals(Map(vx -> (3 * vz), vy -> (vz * vx), vz -> (vz + vy))) ==
        (1 + (3 * vz)) * (vz * vx) - ((vz + vy) * 31 - 2)) 
  }
  
  test("Simple simplify") {
    val ctx = new Z3Context
    val x = ctx.mkBVConst("x", 32);
    val y = ctx.mkBVConst("y", 32);
    val z = ctx.mkBVConst("z", 32);

    println(Symbolic(Const(1, 32) + Const(2, 32)).simplify())
  }

}
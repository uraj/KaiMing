package edu.psu.ist.plato.kaiming.test.ir

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

import com.microsoft.z3.{Context => Z3Context}
import com.microsoft.z3.{BitVecExpr => Z3BVExpr}
import com.microsoft.z3.{BitVecNum => Z3BVNum}

import edu.psu.ist.plato.kaiming.utils.Exception

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
  
  test("Simple flattening AArch64") {
    
    import edu.psu.ist.plato.kaiming.aarch64._
    
    val program = """0x0000 _test:
    0x0000 mov X1, #12
    0x0004 mov X2, X1
    0x0008 add X3, X2, #67
    0x000c subs X5, X3, X1
    0x0010 b.eq 0x001c
    0x0014 mov X6, #1234
    0x0018 b 0x0020
    0x001c mov X6, #1235
    0x0020 mov X7, X6
    """
    
    val result: (Option[List[Function]], String) = 
        AArch64Parser.parseAll(AArch64Parser.binaryunit, program) match {
          case AArch64Parser.Success(value, _) => (Some(value), "")
          case AArch64Parser.NoSuccess(msg, next) =>
            Console.err.println(msg + " " +  next.offset + " " + next.pos)
            (None, msg + " " +  next.offset + " " + next.pos)
        }
    
    val ctx = result match {
      case (Some(func::xs), _) =>
        new Context(func)
      case _ => Exception.unreachable()
    }
    
    IRPrinter.out.printContextWithUDInfo(ctx)
    val bbs = ctx.cfg.iterator.toVector
    
    val stmt1 = bbs(3).entries(0)
    assert(ctx.flattenExpr(stmt1, Reg(Register.Id.X6)) == None)
    
    val stmt2 = bbs(0).entries(3)
    println(stmt2)
    println(ctx.flattenExpr(stmt2, Reg(Register.Id.X3) - Reg(Register.Id.X1)))
  }
  
  test("Simple simplify") {
    val ctx = new Z3Context
    val x = ctx.mkBVConst("x", 32);
    val y = ctx.mkBVConst("y", 32);
    val z = ctx.mkBVConst("z", 32);

    println(Symbolic(Const(1, 32) + Const(2, 32)).simplify())
  }

}
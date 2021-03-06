package io.github.uraj.kaiming.test.ir

import scala.language.implicitConversions

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

import com.microsoft.z3.{Context => Z3Context}
import com.microsoft.z3.{BitVecExpr => Z3BVExpr}
import com.microsoft.z3.{BitVecNum => Z3BVNum}

import io.github.uraj.kaiming.ir.{Context, Var, Const, Reg, IRPrinter, Symbolic}
import io.github.uraj.kaiming.utils.Exception

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
    
    import io.github.uraj.kaiming.aarch64._
    
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
    0x0024 adds X7, X7, #1
    0x0028 b.lo 0x0024
    0x002c mov X8, X7
    """
    
    val result: (Option[Seq[Function]], String) = 
        AArch64Parser.binaryunit.parse(program) match {
          case fastparse.all.Parsed.Success(value, _) => (Some(value), "")
          case f: fastparse.all.Parsed.Failure =>
            Console.err.println(f.msg)
            (None, f.msg)
        }
    
    val ctx = result match {
      case (Some(funcs), _) if funcs.length == 1 =>
        new Context(funcs(0))
      case _ => Exception.unreachable()
    }
    
    IRPrinter.out.printContextWithUDInfo(ctx)
    val bbs = ctx.cfg.iterator.toVector
    
    val stmt1 = bbs(3).entries(0)
    assert(ctx.flattenExpr(stmt1, Reg(Register.Id.X6)) == None)
    
    val stmt2 = bbs(0).entries(3)
    val flattened = ctx.flattenExpr(stmt2, Reg(Register.Id.X3) - Reg(Register.Id.X1))
    assert(flattened.isDefined)
    assert(Symbolic(flattened.get).simplify.asInstanceOf[Z3BVNum].getInt == 0x43)
    
    val stmt3 = bbs(5).entries(0)
    assert(ctx.hasCyclicDefinition(stmt3, Reg(Register.Id.X7)))
    
  }
  
  test("Simple solving") {
    val ctx = new Z3Context
    val x = Var(null, "x", 32)
    val y = Var(null, "y", 32)

    val equation = 
      ctx.mkEq(ctx.mkBVSRem(Symbolic(x * (x + Const(1, 32)), ctx), ctx.mkBV(2, 32)), ctx.mkBV(1, 32))
      
    val solver = ctx.mkSolver
    solver.add(equation)
    
    assert(solver.check.toInt == -1)
  }

}

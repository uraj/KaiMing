package edu.psu.ist.plato.kaiming.test.x86.ir.typing

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.scalatest.junit.JUnitRunner

import edu.psu.ist.plato.kaiming.x86.ir.typing._

import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TestConstraintSolving extends FunSuite with BeforeAndAfter {
  test("Testing cycle elimination") {
    val solver = new ConstraintSolver()
    val t1 = new TypeVar(1)
    val t2 = new TypeVar(2)
    val t3 = new TypeVar(3)
    val t4 = new TypeVar(4)
    val t5 = new TypeVar(5)
    solver.add(Constraint(t1, t2))
    solver.add(Constraint(t2, t3))
    solver.add(Constraint(t3, t2))
    solver.add(Constraint(t3, t4))
    solver.add(Constraint(t4, t3))
    solver.add(Constraint(t3, t5))
    solver.add(Constraint(t3, t1))
    val g = solver.solve
    println(g)
  }
}
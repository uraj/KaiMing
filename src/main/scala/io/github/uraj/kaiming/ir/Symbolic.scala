package io.github.uraj.kaiming.ir

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

import scala.language.implicitConversions

import com.microsoft.z3.{Context => Z3Context}
import com.microsoft.z3.{Expr => Z3Expr}
import com.microsoft.z3.{BoolExpr => Z3BoolExpr}
import com.microsoft.z3.{BitVecExpr => Z3BVExpr}
import com.microsoft.z3.{BitVecNum => Z3BVNum}
import io.github.uraj.kaiming.utils.Exception

object Symbolic {

  private val versionString = "0.1"
  
  private val DS = File.separator
  private val PS = File.pathSeparator
  private val LIB_BIN = DS + "lib-native" + DS
  private val rawLibNames = List("z3", "z3java")
  private val isWindows = System.getProperty("os.name").startsWith("Windows")
  
  private val libsToLoad =
    if (isWindows) rawLibNames.map("lib" + _) else rawLibNames

  private def loadFromJar {
    val path = "KaiMing_" + versionString
    val libDir  = new File(System.getProperty("java.io.tmpdir") + DS + path + LIB_BIN)

    try {
      if (!libDir.isDirectory || !libDir.canRead) {
        libDir.mkdirs
        copyNativesToPath(libDir)
      }
      addLibraryPath(libDir.getAbsolutePath)
    } catch {
      case e: Exception =>
        Console.err.println(e.getMessage)
        e.printStackTrace
        System.exit(1)
    }
    
    if (System.getProperty("os.name").startsWith("Windows")) {
      System.loadLibrary("libz3")
      System.loadLibrary("libz3java")
    }
  }

  private def addLibraryPath(pathToAdd: String) {
    System.setProperty("java.library.path",
        pathToAdd + PS + System.getProperty("java.library.path"))

    // Force JVM to reload "java.library.path" property
    val field = classOf[ClassLoader].getDeclaredField("sys_paths")
    field.setAccessible(true)
    field.set(null, null)
  }

  private def copyNativesToPath(toDir: File) {
    for (lib <- libsToLoad) {
      val name = System.mapLibraryName(lib)
      val to = new File(toDir.getAbsolutePath + DS + name)
      val in = getClass.getResourceAsStream(LIB_BIN + name)
      val out = new FileOutputStream(to)
      val buf = Array.fill[Byte](4096)(0)
      var len = 0
      len = in.read(buf)
      while (len > 0) {
        out.write(buf, 0, len)
        len = in.read(buf)
      }
      out.close
      in.close
    }
  }
  
  loadFromJar
  
  // A hook to trigger environment initialization
  val init = Unit
  
  private class ASTBuilder(val ctx: Z3Context)
    extends Expr.NopVisitor[Z3BVExpr] {
    
    private def reverse(size: Int, granularity: Int, value: Z3BVExpr,
        pos: Int): Z3BVExpr = {
      if (pos + granularity == size)
        ctx.mkExtract(size - 1, pos, value)
      else {
        ctx.mkConcat(ctx.mkExtract(pos + granularity - 1, pos, value),
            reverse(size, granularity, value, pos + granularity))
      }
    }
    
    protected def merge(be: BExpr, left: Z3BVExpr, right: Z3BVExpr) =
      be match {
        case add: Add => ctx.mkBVAdd(left, right)
        case sub: Sub => ctx.mkBVSub(left, right)
        case concat: Concat => ctx.mkConcat(left, right)
        case and: And => ctx.mkBVAND(left, right)
        case or: Or => ctx.mkBVOR(left, right)
        case xor: Xor => ctx.mkBVXOR(left, right)
        case ror: Ror => ctx.mkBVRotateRight(left, right)
        case sext: SExt => ctx.mkSignExt(right.asInstanceOf[Z3BVNum].getInt, left)
        case uext: UExt => ctx.mkZeroExt(right.asInstanceOf[Z3BVNum].getInt, left)
        case sdiv: SDiv => ctx.mkBVSDiv(left, right)
        case udiv: UDiv => ctx.mkBVUDiv(left, right)
        case mul: Mul => ctx.mkBVMul(left, right)
        case shr: Shr => ctx.mkBVLSHR(left, right)
        case shl: Shl => ctx.mkBVSHL(left, right)
        case sar: Sar => ctx.mkBVASHR(left, right)
        case high: High =>
          ctx.mkExtract(left.getSortSize - 1,
              right.asInstanceOf[Z3BVNum].getInt, left)
        case low: Low =>
          ctx.mkExtract(right.asInstanceOf[Z3BVNum].getInt - 1,
              0, left)
        case bswap: BSwap =>
          val granularity = right.asInstanceOf[Z3BVNum].getInt
          reverse(be.sizeInBits, granularity, left, 0)
      }
    
    protected def lift(ue: UExpr, sub: Z3BVExpr) = {
      val t = ctx.mkBV(1, 1)
      val f = ctx.mkBV(0, 1)
      implicit def toBitNeg(e: Z3BoolExpr) = ctx.mkITE(e, f, t).asInstanceOf[Z3BVExpr]
      def toBit(e: Z3BoolExpr) = ctx.mkITE(e, t, f).asInstanceOf[Z3BVExpr]
      ue match {
        case not: Not => ctx.mkBVNot(sub)
        case neg: Neg => toBitNeg(ctx.mkEq(sub, ctx.mkBV(0, sub.getSortSize)))
        case clz: CLeadingZero => Exception.unsupported()
        case ee: ExtractorExpr => {
          val zero = ctx.mkBV(0, ue.sizeInBits)
          val args = sub.getArgs
          val arg0 = args(0).asInstanceOf[Z3BVExpr]
          val arg1 = args(1).asInstanceOf[Z3BVExpr]
          ee match {
            case Carry(ce) => ce match {
              case Add(l, r) =>
                ctx.mkBVAddNoOverflow(arg0, arg1, false)
              case Sub(l, r) =>
                ctx.mkBVSubNoUnderflow(arg0, arg1, false)
              case _ => Exception.unreachable("carry for " + ce + " is undefined")
            }
            case Overflow(oe) => oe match {
              case Add(l, r) =>
                ctx.mkAnd(ctx.mkBVAddNoUnderflow(arg0, arg1),
                    ctx.mkBVAddNoOverflow(arg0, arg1, true))
              case Sub(l, r) =>
                ctx.mkAnd(ctx.mkBVSubNoOverflow(arg0, arg1),
                    ctx.mkBVSubNoUnderflow(arg0, arg1, true))
              case _ => Exception.unreachable("overflow for " + oe + " is undefined")
            }
            case Zero(_) =>
              toBit(ctx.mkEq(zero, sub))
            case Negative(_) =>
              ctx.mkExtract(ue.sizeInBits - 1, ue.sizeInBits - 1, sub)
          }
        }
      }
    }
    
    override def visitConst(in: Z3BVExpr, c: Const) = 
      SkipChildren(ctx.mkBV(c.value, c.sizeInBits))

    override def visitLval(in: Z3BVExpr, lv: Lval) =
      SkipChildren(ctx.mkBVConst(lv.name, lv.sizeInBits))
      
  }
  
  def apply(e: Expr, ctx: Z3Context = new Z3Context) =
    new ASTBuilder(ctx).visit(null, e)._1
  
}

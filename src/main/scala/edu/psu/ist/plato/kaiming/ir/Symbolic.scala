package edu.psu.ist.plato.kaiming.ir

import java.io.File
import java.io.FileOutputStream

import java.util.zip.ZipInputStream

import com.microsoft.z3.{Context => Z3Context}
import com.microsoft.z3.{BitVecExpr => Z3BVExpr}
import com.microsoft.z3.{BitVecNum => Z3BVNum}

import edu.psu.ist.plato.kaiming.exception.UnreachableCodeException

object Symbolic {

  private val DS = File.separator
  private val PS = File.pathSeparator
  private val LIB_BIN = DS + "lib-native" + DS
  private val versionString = "0.1"
  
  private val libsToLoad = List("z3", "z3java")

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
        System.exit(1)
    }
  }

  def addLibraryPath(pathToAdd: String) {
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
    
    def merge(be: BExpr, left: Z3BVExpr, right: Z3BVExpr) =
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
      }
    
    def lift(ue: UExpr, sub: Z3BVExpr) =
      ue match {
        case not: Not => ctx.mkBVNot(sub)
        case bswap: BSwap => 
          ue.sizeInBits match {
            case 8 => sub
            case 16 =>
              ctx.mkConcat(ctx.mkExtract(7, 0, sub), ctx.mkExtract(15, 8, sub))
            case 32 =>
              ctx.mkConcat(ctx.mkExtract(7, 0, sub), ctx.mkConcat(ctx.mkExtract(15, 8, sub),
                  ctx.mkConcat(ctx.mkExtract(23, 16, sub), ctx.mkExtract(31, 24, sub))))
            case 64 =>
              ctx.mkConcat(ctx.mkExtract(7, 0, sub),
                ctx.mkConcat(ctx.mkExtract(15, 8, sub),
                  ctx.mkConcat(ctx.mkExtract(23, 16, sub),
                    ctx.mkConcat(ctx.mkExtract(31, 24, sub),
                      ctx.mkConcat(ctx.mkExtract(39, 32, sub),
                        ctx.mkConcat(ctx.mkExtract(47, 40, sub),
                          ctx.mkConcat(ctx.mkExtract(55, 48, sub),
                            ctx.mkExtract(63, 56, sub))))))))
            case _ => throw new UnreachableCodeException
          }
      }
    
    override def visitConst(in: Z3BVExpr, c: Const) = 
      SkipChildren(ctx.mkBV(c.value, c.sizeInBits))

    override def visitLval(in: Z3BVExpr, lv: Lval) =
      SkipChildren(ctx.mkBV(lv.name, lv.sizeInBits))
      
  }
  
  def apply(e: Expr, ctx: Z3Context = new Z3Context) = {
    new ASTBuilder(ctx).visit(null, e)._1
  }
  
}
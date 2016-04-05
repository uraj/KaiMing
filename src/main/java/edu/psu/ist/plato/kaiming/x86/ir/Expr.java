package edu.psu.ist.plato.kaiming.x86.ir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.x86.Immediate;
import edu.psu.ist.plato.kaiming.x86.Memory;
import edu.psu.ist.plato.kaiming.x86.Register;

public abstract class Expr {
    
    public boolean isLval() {
        return false;
    };
    
    public abstract Expr getSubExpr(int index);
    
    public abstract int getNumSubExpr();
    
    public static Const toExpr(Immediate imm) {
        return Const.getConstant(imm);
    }
    
    public static Const toExpr(int imm) {
        return Const.getConstant(imm);
    }
    
    public static Expr toExpr(Register reg) {
        return Reg.getReg(reg);
    }
    
    public static Expr toExpr(Memory mem) {
        Expr ret = null;
        if (mem.getOffsetRegister() != null) {
            ret = toExpr(mem.getOffsetRegister());
            if (mem.getMultiplier() != 1) {
                ret = new BExpr(BExpr.Op.UMUL, ret, Const.getConstant(mem.getMultiplier()));
            }
        }
        if (mem.getBaseRegister() != null) {
            if (ret == null) {
                ret = toExpr(mem.getBaseRegister());
            } else {
                ret = new BExpr(BExpr.Op.UADD, toExpr(mem.getBaseRegister()), ret);
            }
        }
        if (mem.getDisplacement() != 0) {
            if (ret == null) {
                ret = Const.getConstant(mem.getDisplacement());
            } else {
                ret = new BExpr(BExpr.Op.UADD, Const.getConstant(mem.getDisplacement()), ret);
            }
        }
        if (ret == null)
            ret = Const.getConstant(mem.getDisplacement());
        return ret;
    }
    
    public static abstract class Visitor {

        private boolean action(Expr expr) {
            if (expr instanceof BExpr) {
                return visitBExpr((BExpr)expr);
            } else if (expr instanceof UExpr) {
                return visitBExpr((BExpr)expr);
            } else if (expr instanceof Lval) {
                if (!visitLval((Lval)expr))
                    return false;
                if (expr instanceof Reg) {
                    return visitReg((Reg)expr);
                } else if (expr instanceof Var) {
                    return visitVar((Var)expr);
                } else {
                    Assert.unreachable();
                    return false;
                }
            } else if (expr instanceof Const) {
                return visitConst((Const)expr);
            } else {
                System.err.println(expr);
                Assert.unreachable();
                return false;
            }
        }

        public void visit(Expr toVisit) {
            doVisit(toVisit);
        }
        
        private boolean doVisit(Expr toVisit) {
            if (!action(toVisit))
                return false;
            for (int i = 0; i < toVisit.getNumSubExpr(); ++i) {
                visit(toVisit.getSubExpr(i));
            }
            return true;
        }
        
        protected boolean visitBExpr(BExpr expr) { return true; };
        protected boolean visitUExpr(UExpr expr) { return true; };
        protected boolean visitLval(Lval lval) { return true; };
        protected boolean visitVar(Var lval) { return true; };
        protected boolean visitReg(Reg lval) { return true; };
        protected boolean visitConst(Const c) { return true; };
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printExpr(this);
        p.close();
        return baos.toString();
    }
}

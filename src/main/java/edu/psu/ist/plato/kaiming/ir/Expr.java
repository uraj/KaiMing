package edu.psu.ist.plato.kaiming.ir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.util.Assert;

// TODO: Implement a builder pattern for expression construction

public abstract class Expr {
    
    public boolean isLval() {
        return false;
    };
    
    public abstract Expr subExpr(int index);
    
    public abstract int numOfSubExpr();
    
    public boolean isPrimitive() {
        return numOfSubExpr() == 0;
    }
    
    public static abstract class Visitor {

        private boolean action(Expr expr) {
            if (!prologue(expr))
                return false;
            
            if (expr instanceof BExpr) {
                return visitBExpr((BExpr)expr);
            } else if (expr instanceof UExpr) {
                return visitUExpr((UExpr)expr);
            } else if (expr instanceof Reg) {
                return visitReg((Reg)expr);
            } else if (expr instanceof Var) {
                return visitVar((Var)expr);
            } else if (expr instanceof Flg) {
                return visitFlg((Flg)expr);
            }  else if (expr instanceof Const) {
                return visitConst((Const)expr);
            } else {
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
            for (int i = 0; i < toVisit.numOfSubExpr(); ++i) {
                visit(toVisit.subExpr(i));
            }
            return true;
        }

        protected boolean prologue(Expr expr) { return true; }
        protected boolean visitBExpr(BExpr expr) { return true; };
        protected boolean visitUExpr(UExpr expr) { return true; };
        protected boolean visitVar(Var lval) { return true; };
        protected boolean visitReg(Reg lval) { return true; };
        protected boolean visitFlg(Flg lval) { return true; };
        protected boolean visitConst(Const c) { return true; };
    }

    @Override
    public final String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printExpr(this);
        p.close();
        return baos.toString();
    }
    
    public Target asTarget(BasicBlock<Stmt> bb) {
        return new Target(this, bb);
    }
    
    @Override
    public abstract boolean equals(Object that);
    
    @Override
    public abstract int hashCode();
}

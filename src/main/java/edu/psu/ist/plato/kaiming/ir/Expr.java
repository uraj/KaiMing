package edu.psu.ist.plato.kaiming.ir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.util.Assert;

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
    
    public static class LvalProbe extends Visitor {
        
        private Set<Lval> mLvals = new HashSet<Lval>();
        private boolean mExpired = false;
        
        @Override
        protected boolean visitReg(Reg lval) {
            mLvals.add(lval);
            return true;
        }
        
        @Override
        protected boolean visitVar(Var lval) {
            mLvals.add(lval);
            return true;
        }
        
        @Override
        protected boolean visitFlg(Flg lval) {
            mLvals.add(lval);
            return true;
        }
        
        public Set<Lval> probedLvals() {
            // Since java use mutable collections, we want
            // to make sure only one client has access to
            // the returned value
            if (mExpired)
                throw new UnsupportedOperationException();
            mExpired = true;
            return mLvals;
        }
    }
    
    public static Set<Lval> enumLvals(Expr e) {
        LvalProbe lp = new LvalProbe();
        lp.visit(e);
        return lp.probedLvals();
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
    
    public abstract Expr substitute(Expr o, Expr n);
    
    public abstract boolean contains(Expr o);
    
    @Override
    public abstract boolean equals(Object that);
    
    @Override
    public abstract int hashCode();
    
    public BExpr add(Expr that) {
    	return new BExpr(BExpr.Op.ADD, this, that);
    }
    
    public BExpr sub(Expr that) {
    	return new BExpr(BExpr.Op.SUB, this, that);
    }
    
    public BExpr and(Expr that) {
    	return new BExpr(BExpr.Op.AND, this, that);
    }
    
    public BExpr or(Expr that) {
    	return new BExpr(BExpr.Op.AND, this, that);
    }
    
    public BExpr xor(Expr that) {
    	return new BExpr(BExpr.Op.XOR, this, that);
    }
    
    public BExpr shl(Expr that) {
    	return new BExpr(BExpr.Op.SHL, this, that);
    }
    
    public BExpr shr(Expr that) {
    	return new BExpr(BExpr.Op.SHR, this, that);
    }
    
    public BExpr sar(Expr that) {
    	return new BExpr(BExpr.Op.SAR, this, that);
    }
    
    public BExpr mul(Expr that) {
    	return new BExpr(BExpr.Op.MUL, this, that);
    }
    
    public BExpr div(Expr that) {
    	return new BExpr(BExpr.Op.DIV, this, that);
    }
    
    public BExpr concat(Expr that) {
    	return new BExpr(BExpr.Op.CONCAT, this, that);
    }
    
    public BExpr ror(Expr that) {
    	return new BExpr(BExpr.Op.ROR, this, that);
    }
    
    public BExpr sext(Expr that) {
    	return new BExpr(BExpr.Op.SEXT, this, that);
    }
    
    public BExpr uext(Expr that) {
    	return new BExpr(BExpr.Op.UEXT, this, that);
    }
    
    public UExpr low() {
    	return new UExpr(UExpr.Op.LOW, this);
    }
    
    public UExpr high() {
    	return new UExpr(UExpr.Op.HIGH, this);
    }
    
    public UExpr bswap() {
    	return new UExpr(UExpr.Op.BSWAP, this);
    }

    public UExpr not() {
    	return new UExpr(UExpr.Op.NOT, this);
    }
    
    public UExpr foverflow() {
    	return new UExpr(UExpr.Op.OVERFLOW, this);
    }
    
    public UExpr fnegative() {
    	return new UExpr(UExpr.Op.NEGATIVE, this);
    }
    
    public UExpr fzero() {
    	return new UExpr(UExpr.Op.ZERO, this);
    }
    
    public UExpr fcarry() {
    	return new UExpr(UExpr.Op.CARRY, this);
    }
    
}

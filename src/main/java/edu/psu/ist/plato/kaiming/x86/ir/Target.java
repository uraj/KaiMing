package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.Label;

public final class Target extends Expr {

    Expr mExpr;
    BasicBlock<Stmt> mBB;
    
    protected Target (Expr e, BasicBlock<Stmt> target) {
        mExpr = e;
        mBB = target;
    }
    
    @Override
    public Expr subExpr(int index) {
        return mExpr;
    }

    @Override
    public int numOfSubExpr() {
        return 1;
    }
    
    public Expr underlyingExpr() {
        return mExpr;
    }
    
    public BasicBlock<Stmt> target() {
        return mBB;
    }
    
    public void setTarget(BasicBlock<Stmt> target) {
        mBB = target;
    }
    
    public Label targetLabel() {
        return mBB.label();
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof Target) {
            return mExpr.equals(((Target)that).mExpr);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mExpr.hashCode();
    }
}

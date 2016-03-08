package edu.psu.ist.plato.kaiming.x86.ir;

public abstract class Expr {
    public boolean isLval() {
        return false;
    };
    
    public Expr getSubExpr(int index) {
        return null;
    };
    
    public abstract int getNumSubExpr();
}

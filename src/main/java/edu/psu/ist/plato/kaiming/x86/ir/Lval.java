package edu.psu.ist.plato.kaiming.x86.ir;

public abstract class Lval extends Expr {

    @Override
    final public boolean isLval() {
        return true;
    }

    @Override
    public int getNumSubExpr() {
        return 0;
    }
    
    @Override
    public Expr getSubExpr(int index) {
    	return null;
    }

}

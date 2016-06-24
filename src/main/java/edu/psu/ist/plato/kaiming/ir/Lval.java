package edu.psu.ist.plato.kaiming.ir;

public abstract class Lval extends Expr {

    @Override
    final public boolean isLval() {
        return true;
    }

    @Override
    public int numOfSubExpr() {
        return 0;
    }
    
    @Override
    public Expr subExpr(int index) {
    	return null;
    }
    
    @Override
    public abstract int hashCode();
    
    @Override
    public abstract boolean equals(Object that);
    
    public abstract int sizeInBits();

}

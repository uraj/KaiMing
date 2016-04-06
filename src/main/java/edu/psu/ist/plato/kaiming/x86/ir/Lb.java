package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.Label;

public class Lb extends Expr {

    Expr mExpr;
    Label mLabel;
    
    public Lb (Expr e, Label l) {
        mExpr = e;
        mLabel = l;
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
    
    public Label label() {
        return mLabel;
    }

}

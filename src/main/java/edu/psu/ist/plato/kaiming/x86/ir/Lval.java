package edu.psu.ist.plato.kaiming.x86.ir;

public class Lval extends Expr {

    @Override
    final public boolean isLval() {
        return true;
    }
}

package edu.psu.ist.plato.kaiming.x86.ir;

public class Var extends Lval {
    private String mName;

    public Var(String name) {
        mName = name;
    }
    
    public String getName() {
        return mName;
    }
}

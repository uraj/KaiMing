package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Flag;

public class Flg extends Expr {
    public final Flag flag;
    
    public Flg(Flag f) {
        flag = f;
    }

	@Override
	public Expr getSubExpr(int index) {
		return null;
	}

	@Override
	public int getNumSubExpr() {
		return 0;
	}
}

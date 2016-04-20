package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Flag;

public final class Flg extends Lval {
    public final Flag flag;
    
    public Flg(Flag f) {
        flag = f;
    }

	@Override
	public Expr subExpr(int index) {
		return null;
	}

	@Override
	public int numOfSubExpr() {
		return 0;
	}

    @Override
    public boolean equals(Object lv) {
        if (lv instanceof Flg) {
            return ((Flg)lv).flag == flag;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return flag.hashCode();
    }
    
    @Override
    public int sizeInBits() {
        return 1;
    }
}

package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.Machine.MachFlag;

public final class Flg extends Lval {
    public final MachFlag flag;
    
    private Flg(MachFlag f) {
        flag = f;
    }
    
    public static Flg getFlg(MachFlag f) {
        return new Flg(f);
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

package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Immediate;
import edu.psu.ist.plato.kaiming.x86.Memory;
import edu.psu.ist.plato.kaiming.x86.Register;

public abstract class Expr {
    public boolean isLval() {
        return false;
    };
    
    public abstract Expr getSubExpr(int index);

    
    public abstract int getNumSubExpr();
    
    public static Const toExpr(Immediate imm) {
    	return Const.getConstant(imm);
    }
    
    public static Expr toExpr(Register reg) {
    	if (reg.id == Register.Id.EIZ)
    		return Const.getConstant(0);
    	else
    		return new Reg(reg);
    }
    
    public static Expr toExpr(Memory mem) {
    	Expr ret = null;
    	if (mem.getOffsetRegister() != null) {
    		ret = toExpr(mem.getOffsetRegister());
    		if (mem.getMultiplier() != 1) {
        		ret = new BinaryExpr(BinaryExpr.Op.MUL, ret, Const.getConstant(mem.getMultiplier()));
        	}
    	}
    	if (mem.getBaseRegister() != null) {
    		if (ret == null) {
    			ret = toExpr(mem.getBaseRegister());
    		} else {
    			ret = new BinaryExpr(BinaryExpr.Op.ADD, toExpr(mem.getBaseRegister()), ret);
    		}
    	}
    	if (mem.getDisplacement() != 0) {
    		if (ret == null) {
    			ret = Const.getConstant(mem.getDisplacement());
    		} else {
    			ret = new BinaryExpr(BinaryExpr.Op.ADD, Const.getConstant(mem.getDisplacement()), ret);
    		}
    	}
    	return ret;
    }

}

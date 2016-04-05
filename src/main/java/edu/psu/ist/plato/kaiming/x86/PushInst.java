package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.util.Assert;

public class PushInst extends Instruction {

    private final int mSizeInBits;
    
    protected PushInst(long addr, Opcode op, Operand source) {
        super(Kind.PUSH, addr, op, new Operand[] { source } );
        mSizeInBits = getSizeInBits(mOpcode, getOperand());
    }

    public Operand getOperand() {
        return getOperand(0);
    }
    
    public int sizeInBits() {
        return mSizeInBits;
    }
    
    private int getSizeInBits(Opcode opcode, Operand op) {
    	String rawop = opcode.getRawOpcode();
    	if (op.isRegister()) {
    		return op.asRegister().getSizeInBits();
    	} else {
    		if (rawop.endsWith("l")) {
    			return 32;
    		} else if (rawop.endsWith("w")) {
    			return 16;
    		} else if (rawop.endsWith("b")) {
    			return 8;
    		} else {
    		    Assert.test(op.isImmeidate());
    		    // FIXME: This is actually incorrect. The default
    		    // operand size is not encoded in the instruction
    		    // but depends on processor mode.  
    			return 32;
    		}
    	}
    }
}

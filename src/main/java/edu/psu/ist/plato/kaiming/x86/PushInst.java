package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.util.Assert;

public class PushInst extends Instruction {

    protected PushInst(long addr, Opcode op, Operand source) {
        super(addr, op, new Operand[] { source } );
    }

    public Operand getOperand() { return getOperand(0); }
    
    // TODO: generalize this to all instructions
    public int getOperandSizeInBytes() {
    	String rawop = mOpcode.getRawOpcode();
    	Operand op = getOperand();
    	if (op.isRegister()) {
    		return op.asRegister().getSizeInBits() / 8;
    	} else {
    		if (rawop.endsWith("l")) {
    			return 4;
    		} else if (rawop.endsWith("w")) {
    			return 2;
    		} else if (rawop.endsWith("b")) {
    			return 1;
    		} else {
    			Assert.unreachable();
    			return -1;
    		}
    	}
    }
}

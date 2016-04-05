package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.util.Assert;

public class MoveStrInst extends Instruction {

    private int mSizeInBits;
    
    protected MoveStrInst(long addr, Opcode op, Memory from, Memory to) {
        super(Kind.MOVE_STR, addr, op, new Operand[] {from, to});
        String code = op.getRawOpcode();
        if (code.endsWith("l")) {
            mSizeInBits = 32;
        } else if (code.endsWith("w")) {
            mSizeInBits = 16;
        } else if (code.endsWith("b")) {
            mSizeInBits = 8;
        } else {
            Assert.unreachable("Ambigious movs instruction");
        }
    }
    
    public int moveSizeInBits() {
        return mSizeInBits;
    }
    
    public Memory fromAddr() {
        return (Memory)getOperand(0);
    }
    
    public Memory toAddr() {
        return (Memory)getOperand(1);
    }

}

package edu.psu.ist.plato.kaiming.x86;

import java.util.Arrays;

import edu.psu.ist.plato.kaiming.util.Tuple;

//TODO The source and destination are decided by operand length
public class MultiplyInst extends Instruction {

    private Operand[] mDest;
    private Operand[] mSrc;
    
    private static final Operand[] sDefaultDest = 
            new Operand[] { Register.getRegister(Register.Id.EAX), 
                            Register.getRegister(Register.Id.EDX)};
    
    protected MultiplyInst(long addr, Opcode op, Operand[] operands) {
        super(Kind.MULTIPLY, addr, op, operands);
        switch (operands.length) {
            case 1:
                mDest = sDefaultDest;
                mSrc = new Operand[] {operands[0], 
                        Register.getRegister(Register.Id.EAX)};
                break;
            case 2:
                mDest = new Operand[] {operands[1]};
                mSrc = new Operand[] {operands[0]};
                break;
            case 3:
                mDest = new Operand[] {operands[2]};
                mSrc = new Operand[] {operands[0], operands[1]};
                break;
            default:
                throw new IllegalArgumentException(
                        "mul instructiona only accepts 1, 2, or 3 operands");
        }
    }
    
    public Tuple<Operand, Operand> getDest() {
        return new Tuple<Operand, Operand>(mDest[0], mDest.length == 1 ? null : mDest[1]);
    }
    
    public Tuple<Operand, Operand> getSrc() {
        return new Tuple<Operand, Operand>(mSrc[0], mSrc[1]);
    }
    
    public Operand[] iterDest() {
        return Arrays.copyOf(mDest, mDest.length);
    }
    
    public Operand[] iterSrc() {
        return Arrays.copyOf(mSrc, mSrc.length);
    }

}

package edu.psu.ist.plato.kaiming.arm64;

import java.util.ArrayList;
import java.util.List;

public class PopInst extends Instruction {

    protected PopInst(long addr, Opcode op, List<Register> operands) {
        super(Kind.POP, addr, op, operands.toArray(new Operand[] {}));
    }
    
    public int numOfPoppedRegisters() {
        return numOfOperands();
    }
    
    public List<Register> poppedRegisters() {
        int i = numOfOperands();
        List<Register> ret = new ArrayList<>(i);
        for (int j = 0; j < i; ++j) {
            ret.add(operand(j).asRegister());
        }
        return ret;
    }
    
    public Register firstPoppedRegister() {
        return operand(0).asRegister();
    }

}

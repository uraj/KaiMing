package edu.psu.ist.plato.kaiming.arm64;

import java.util.ArrayList;
import java.util.List;

public class PushInst extends Instruction {

    protected PushInst(long addr, Opcode op, List<Register> operands) {
        super(Kind.PUSH, addr, op, operands.toArray(new Operand[] {}));
    }

    public int numOfPushedRegisters() {
        return numOfOperands();
    }
    
    public List<Register> pushedRegisters() {
        int i = numOfOperands();
        List<Register> ret = new ArrayList<>(i);
        for (int j = 0; j < i; ++j) {
            ret.add(operand(j).asRegister());
        }
        return ret;
    }
    
    public Register firstPushedRegister() {
        return operand(0).asRegister();
    }

}

package edu.psu.ist.plato.kaiming.x86;

// TODO The dividend and destination are decided by operand length 
public class DivideInst extends Instruction {

    protected DivideInst(long addr, Opcode op, Operand operand) {
        super(addr, op, new Operand[] {operand});
    }
    
    public Register[] getDividend() {
        return new Register[] { Register.getRegister(Register.Id.EAX),
                Register.getRegister(Register.Id.EDX)};
    }
    
    public Operand getDivider() {
        return getOperand(0);
    }
    
    public Register[] getDest() {
        return new Register[] { Register.getRegister(Register.Id.EAX),
                Register.getRegister(Register.Id.EDX)};
    }
}

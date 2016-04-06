package edu.psu.ist.plato.kaiming.x86;

import java.util.ArrayList;

import edu.psu.ist.plato.kaiming.util.Tuple;

// TODO The dividend and destination are decided by operand length 
public class DivideInst extends Instruction {

    protected DivideInst(long addr, Opcode op, Operand operand) {
        super(Kind.DIVIDE, addr, op, new Operand[] {operand});
    }
    
    public Tuple<Register, Register> dividend() {
        return new Tuple<Register, Register>(Register.getRegister(Register.Id.EAX),
                Register.getRegister(Register.Id.EDX));
    }
    
    public Iterable<Register> dividendIterator() {
    	ArrayList<Register> ret = new ArrayList<Register>();
    	ret.add(Register.getRegister(Register.Id.EAX));
    	ret.add(Register.getRegister(Register.Id.EDX));
    	return ret;
    }
    
    public Operand divider() {
        return operand(0);
    }
    
    public Tuple<Register, Register> dest() {
        return new Tuple<Register, Register>(Register.getRegister(Register.Id.EAX),
                Register.getRegister(Register.Id.EDX));
    }
    
    public Iterable<Register> destIterator() {
    	ArrayList<Register> ret = new ArrayList<Register>();
    	ret.add(Register.getRegister(Register.Id.EAX));
    	ret.add(Register.getRegister(Register.Id.EDX));
    	return ret;
    }
}

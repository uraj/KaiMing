package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.util.Tuple;

public class ExchangeInst extends Instruction {

    protected ExchangeInst(long addr, Opcode op, Operand xchg1, Operand xchg2) {
        super(Kind.EXCHANGE, addr, op, new Operand[] {xchg1, xchg2});
    }
    
    public Tuple<Operand, Operand> exchangedOperands() {
        return new Tuple<Operand, Operand>(operand(0), operand(1));
    }
    
    public boolean isExchangeAdd() {
        return opcode().rawOpcode().equals("xadd");
    }
}

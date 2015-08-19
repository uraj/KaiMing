package edu.psu.ist.plato.kaiming.x86;

public class ExchangeInst extends Instruction {

    protected ExchangeInst(long addr, Opcode op, Operand xchg1, Operand xchg2) {
        super(addr, op, new Operand[] {xchg1, xchg2});
    }
    
    public Operand[] getExchangedOperands() {
        return new Operand[] {getOperand(0), getOperand(1)};
    }
}

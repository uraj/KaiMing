package edu.psu.ist.plato.kaiming.x86;

public class ExchangeInst extends Instruction {

    protected ExchangeInst(long addr, Opcode op, Operand xchg1, Operand xchg2) {
        super(Kind.EXCHANGE, addr, op, new Operand[] {xchg1, xchg2});
    }
    
    public Operand[] getExchangedOperands() {
        return new Operand[] {getOperand(0), getOperand(1)};
    }
    
    public boolean isExchangeAdd() {
        return getOpcode().getRawOpcode().equals("xadd");
    }
}

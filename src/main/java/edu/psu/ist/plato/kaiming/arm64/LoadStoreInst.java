package edu.psu.ist.plato.kaiming.arm64;

public abstract class LoadStoreInst extends Instruction {

    public enum AddressingMode {
        PRE_INDEX,
        POST_INDEX,
        REGULAR,
    }
    
    private AddressingMode mAddressingMode;
    
    protected LoadStoreInst(Kind kind, long addr, Opcode op, Operand[] operands, AddressingMode mode) {
        super(kind, addr, op, operands);
        mAddressingMode = mode;
    }
    
    public AddressingMode addressingMode() {
        return mAddressingMode;
    }
    
    public abstract int indexingOperandIndex();
    
    public Memory indexingOperand() {
        return operand(indexingOperandIndex()).asMemory();
    }
        
}

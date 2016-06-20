package edu.psu.ist.plato.kaiming.arm64;

public class SelectInst extends CondInstruction {

    protected SelectInst(long addr, Opcode op, Register rd, Register r1, Register r2, Condition cond) {
        super(Kind.SELECT, addr, op, new Operand[] { rd, r1, r2 }, cond);
    }
    
    public boolean incrementSecond() {
        return opcode().mnemonic() == Opcode.Mnemonic.CSINC;
    }

}

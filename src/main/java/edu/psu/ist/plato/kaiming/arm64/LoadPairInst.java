package edu.psu.ist.plato.kaiming.arm64;

public class LoadPairInst extends Instruction {

    protected LoadPairInst(long addr, Opcode op, Register rd1, Register rd2, Memory mem) {
        super(Kind.LOAD_PAIR, addr, op, new Operand[] { rd1, rd2, mem });
    }

}

package edu.psu.ist.plato.kaiming.arm64;

public class BinaryArithInst extends Instruction {

    protected BinaryArithInst(long addr, Opcode op, Register rd, Operand r1, Operand r2) {
        super(Kind.BIN_ARITHN, addr, op, new Operand[] { rd, r1, r2 });
    }

}
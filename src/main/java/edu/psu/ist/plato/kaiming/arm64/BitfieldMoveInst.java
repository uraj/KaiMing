package edu.psu.ist.plato.kaiming.arm64;

import edu.psu.ist.plato.kaiming.util.Assert;

public class BitfieldMoveInst extends Instruction {

    protected BitfieldMoveInst(long addr, Opcode op, Register rd, Register rs,
            Immediate rotate, Immediate idx) {
        super(Kind.BITFIELD_MOVE, addr, op, new Operand[] { rd, rs, rotate, idx });
    }

    protected BitfieldMoveInst(long addr, Opcode op, Register rd, Register rs) {
        super(Kind.BITFIELD_MOVE, addr, op,
                new Operand[] { rd, rs, Immediate.getImmediate(0), null });
        String raw = op.rawOpcode();
        char last = raw.charAt(raw.length() - 1);
        if (last == 'W') {
            setOperand(3, Immediate.getImmediate(31));
        } else if (last == 'H') {
            setOperand(3, Immediate.getImmediate(15));
        } else if (last == 'B') {
            setOperand(3, Immediate.getImmediate(7));
        } else {
            Assert.unreachable();
        }
    }
}
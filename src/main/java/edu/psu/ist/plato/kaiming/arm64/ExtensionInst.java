package edu.psu.ist.plato.kaiming.arm64;

import java.util.Iterator;

import edu.psu.ist.plato.kaiming.util.ArrayIterator;
import edu.psu.ist.plato.kaiming.util.Assert; 

public class ExtensionInst extends BitfieldMoveInst {

    protected ExtensionInst(long addr, Opcode op, Register rd, Register rs) {
        super(addr, op, rd, rs, Immediate.getImmediate(0), null);
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
    
    @Override
    public Iterator<Operand> iterator() {
        return new ArrayIterator<>(new Operand[] { operand(0), operand(1) });
    }

}

package edu.psu.ist.plato.kaiming.arm64;

import java.util.Iterator;

import edu.psu.ist.plato.kaiming.util.ArrayIterator; 

public class ExtensionInst extends BitfieldMoveInst {

    protected ExtensionInst(long addr, Opcode op, Register rd, Register rs) {
        super(addr, op, rd, rs);
    }
    
    @Override
    public Iterator<Operand> iterator() {
        return new ArrayIterator<>(new Operand[] { operand(0), operand(1) });
    }

}

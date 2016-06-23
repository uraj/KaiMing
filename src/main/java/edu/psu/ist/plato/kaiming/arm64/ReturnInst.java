package edu.psu.ist.plato.kaiming.arm64;

import java.util.Collections;
import java.util.Iterator;

public class ReturnInst extends BranchInst {

    private static Register sLinker = Register.get(Register.Id.X30);
    
    protected ReturnInst(long addr, Opcode op) {
        super(addr, op, sLinker);
    }
    
    @Override
    public Iterator<Operand> iterator() {
        return Collections.emptyIterator();
    }

}

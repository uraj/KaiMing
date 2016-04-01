package edu.psu.ist.plato.kaiming.x86;

//FIXME: This is a class for representing uninteresting instructions.
//  Once we have supported all instructions encountered, this class
//  should be removed.
public class OtherInst extends Instruction {

    protected OtherInst(long addr, Opcode op, Operand[] operands) {
        super(Kind.OTHER, addr, op, operands);
    }
    
}

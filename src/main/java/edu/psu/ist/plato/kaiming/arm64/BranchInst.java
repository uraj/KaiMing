package edu.psu.ist.plato.kaiming.arm64;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.Entry;

public class BranchInst extends Instruction implements Entry.Terminator<Instruction> {

    private Condition mCond;
    
    protected BranchInst(long addr, Opcode op, Operand target) {
        super(Kind.BRANCH, addr, op, new Operand[] { target });
        mCond = op.getCondition();
    }
    
    private static Register sLinker = Register.getRegister(Register.Id.X30);
    
    protected BranchInst(long addr, Opcode op) {
        super(Kind.BRANCH, addr, op, new Operand[] { sLinker });
        mCond = op.getCondition();
    }

    public boolean isConditional() {
        return mCond != Condition.AL;
    }
    
    public boolean hasLink() {
        return opcode().mnemonic() == Opcode.Mnemonic.BL;
    }
    
    public boolean isReturn() {
        return opcode().rawOpcode().equals("RET");
    }
    
    public Operand target() {
        return operand(0);
    }

    @Override
    public boolean isInterprocedural() {
        return hasLink() || isReturn();
    }

    @Override
    public boolean isIntraprocedural() {
        return !isInterprocedural();
    }

    @Override
    public boolean isIndirect() {
        return target().isRegister();
    }

    @Override
    public boolean isTargetConcrete() {
        return target().isMemory();
    }

    @Override
    public boolean isCall() {
        return hasLink();
    }

    @Override
    public long targetIndex() {
        return target().asMemory().offset().asImmOff().value;
    }

    @Override
    public void relocateTarget(BasicBlock<Instruction> target) {
        setOperand(0, new Relocation(target));
    }
}

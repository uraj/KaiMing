package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.Entry;

public class ReturnInst extends Instruction implements Entry.Terminator<Instruction> {

    private final static Operand[] sDummyOperands = new Operand[0];
    
    protected ReturnInst(long addr, Opcode op) {
        super(Kind.RETURN, addr, op, sDummyOperands);
    }

	@Override
	public boolean isIntraprocedural() {
		return false;
	}

	@Override
	public boolean isInterprocedural() {
		return true;
	}

	@Override
	public boolean isIndirect() {
		return true;
	}

	@Override
	public boolean isTargetConcrete() {
		return false;
	}

	@Override
	public boolean isReturn() {
		return true;
	}

	@Override
	public boolean isCall() {
		return false;
	}

	@Override
	public long targetIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void relocateTarget(BasicBlock<Instruction> target) {
		throw new UnsupportedOperationException();
	}

}

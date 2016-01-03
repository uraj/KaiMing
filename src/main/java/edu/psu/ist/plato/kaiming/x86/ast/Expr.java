package edu.psu.ist.plato.kaiming.x86.ast;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Label;

public class Expr extends Entry {

	private long mIndex;

	public enum OP {
		VAR,
		GET,
		ADD,
		MUL,
		ST,
		LD,
		REF,
		TEST,
	}

	protected OP mOP;
	protected Expr[] mSubExprs;
	
	protected Expr(OP op, Expr[] elist) {
		mOP = op;
		mSubExprs = elist;
	}
	
	@Override
	public long getIndex() {
		return mIndex;
	}

	@Override
	public int fillLabelInformation(Label l) {
		return 0;
	}

	@Override
	public int fillLabelInformation(Label l, Entry next) {
		return 0;
	}

}

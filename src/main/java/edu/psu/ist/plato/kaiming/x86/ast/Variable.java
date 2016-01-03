package edu.psu.ist.plato.kaiming.x86.ast;

import edu.psu.ist.plato.kaiming.x86.Operand;

public class Variable extends Expr {

	private final Operand mOperand;
	private final String mVarName;
	
	private Variable(String name, Operand op) {
		super(OP.VAR, null);
		mVarName = name;
		mOperand = op;
	}
	
	private Variable(String name) {
		super(OP.VAR, null);
		mVarName = name;
		mOperand = null;
	}
	
	private static int sTmpVarCounter = 0;
	
	public static Variable createTempVar() {
		return new Variable("__tmp_" + sTmpVarCounter++);
	}
	
	public boolean isTemporary() {
		return mVarName.startsWith("__tmp_");
	}
	
	public Operand getUnderlyingOperand() {
		return mOperand;
	}

	public String getVarName() {
		return mVarName;
	}
}

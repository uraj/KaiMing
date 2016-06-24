package edu.psu.ist.plato.kaiming.ir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import java.util.List;

import edu.psu.ist.plato.kaiming.*;
import edu.psu.ist.plato.kaiming.util.Tuple;

public class Context extends Procedure<Stmt> {

    private Procedure<?> mProc;
    private CFG<Stmt> mCFG;
    private int mTempVarCount;
    private Machine mMach;
    
    @Override
    protected CFG<Stmt> buildCFG(List<Stmt> entries) {
    	throw new UnsupportedOperationException();
    }
    
    public Context(edu.psu.ist.plato.kaiming.x86.Function fun) {
        mProc = fun;
        mTempVarCount = 0;
        mMach = Machine.x86;
        Tuple<List<BasicBlock<Stmt>>, BasicBlock<Stmt>> t = Machine.x86.buildCFGForX86(this, fun);
        mCFG = createCFGObject(t.first, t.second);
    }
    
    public Context(edu.psu.ist.plato.kaiming.arm64.Function fun) {
        mProc = fun;
        mTempVarCount = 0;
        mMach = Machine.arm64;
        Tuple<List<BasicBlock<Stmt>>, BasicBlock<Stmt>> t = Machine.arm64.buildCFGForARM64(this, fun);
        mCFG = createCFGObject(t.first, t.second);
    }
    
    public Machine mach() {
        return mMach;
    }
    
    public Var getNewTempVariable() {
        return new Var(this, "temp_" + mTempVarCount++);
    }
    
    public Var getNewTempVariable(int sizeInBits) {
        return new Var(this, "temp_" + mTempVarCount++, sizeInBits);
    }
    
    public Var getNewVariable(String name) {
        return new Var(this, name);
    }
    
	@Override
	protected Label deriveSubLabel(BasicBlock<Stmt> bb) {
		return null;
	}
	
	@Override
    public final String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printContext(this);
        p.close();
        return baos.toString();
    }

    @Override
    public CFG<Stmt> cfg() {
        return mCFG;
    }

    @Override
    public String name() {
        return mProc.name();
    }
    
    public void trackExpression() {
        
    }
}

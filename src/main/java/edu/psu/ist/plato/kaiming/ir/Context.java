package edu.psu.ist.plato.kaiming.ir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Map;

import edu.psu.ist.plato.kaiming.*;
import edu.psu.ist.plato.kaiming.util.Tuple;

public class Context extends Procedure<Stmt> {

    private Procedure<?> mProc;
    private CFG<Stmt> mCFG;
    private Machine mMach;
    
    private int mTempVarCount = 0;
    private int mStmtIndex = 0;
    
    private static String sTempVarPrefix = "__tmp_";
    
    @Override
    protected CFG<Stmt> buildCFG(List<Stmt> entries) {
    	throw new UnsupportedOperationException();
    }
    
    public Context(edu.psu.ist.plato.kaiming.x86.Function fun) {
        mProc = fun;
        mMach = Machine.x86;
        Tuple<List<BasicBlock<Stmt>>, BasicBlock<Stmt>> t = Machine.x86.buildCFGForX86(this, fun);
        mCFG = createCFGObject(t.first, t.second);
    }
    
    public Context(edu.psu.ist.plato.kaiming.arm64.Function fun) {
        mProc = fun;
        mMach = Machine.arm64;
        Tuple<List<BasicBlock<Stmt>>, BasicBlock<Stmt>> t = Machine.arm64.buildCFGForARM64(this, fun);
        mCFG = createCFGObject(t.first, t.second);
    }
    
    public long nextIndex() {
        return mStmtIndex++;
    }
    
    public Machine mach() {
        return mMach;
    }
    
    public Var getNewTempVariable() {
        return new Var(this, sTempVarPrefix + mTempVarCount++);
    }
    
    public Var getNewTempVariable(int sizeInBits) {
        return new Var(this, sTempVarPrefix + mTempVarCount++, sizeInBits);
    }
    
    public Var getNewVariable(String name) {
        if (name.startsWith(sTempVarPrefix))
            throw new IllegalArgumentException(sTempVarPrefix + " is a preserved prefix for variable names");
        return new Var(this, name);
    }
    
	@Override
	protected Label deriveSubLabel(BasicBlock<Stmt> bb) {
		throw new UnsupportedOperationException();
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
    
    public Map<Map<Lval, Set<DefStmt>>, Var> trackExpression(Expr toTrack) {
        Var var = null;
        Map<Lval, Set<DefStmt>> signature = null;
        
        Map<Map<Lval, Set<DefStmt>>, Var> map = new HashMap<>();
        Set<Lval> lvals = Expr.enumLvals(toTrack);
        
        for (BasicBlock<Stmt> bb : cfg()) {
            ListIterator<Stmt> si = bb.iterator();
            while (si.hasNext()) {
                Stmt s = si.next();
                
                signature = null;
                for (int i = 0, e = s.numOfUsedExpr(); i < e; ++i) {
                    Expr used = s.usedExpr(i);
                    if (used.contains(toTrack)) {
                        if (signature == null) {
                            signature = new HashMap<>();
                            for (Lval lv : lvals)
                                signature.put(lv, s.searchDefFor(lv));
                        }
                        
                        var = map.get(signature);
                        if (var == null) {
                            var = getNewTempVariable();
                            map.put(signature, var);
                            // Insert tracker initiation
                            si.previous();
                            Stmt set = new AssignStmt(s.hostEntry(), var, toTrack);
                            set.setIndex(nextIndex());
                            si.add(set);
                            si.next();
                        }
                        
                        Expr replaced = used.substitute(toTrack, var);
                        s.setUsedExpr(i, replaced);
                    }
                }
            }
        }
        return map;
    }
}

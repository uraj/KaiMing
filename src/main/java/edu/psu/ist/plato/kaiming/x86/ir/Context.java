package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Procedure;
import edu.psu.ist.plato.kaiming.x86.Function;
import edu.psu.ist.plato.kaiming.x86.Instruction;

public class Context extends Procedure {

    private Function mFun;
    private CFG mCFG;
    private int mTempVarCount;
    
    public Context(Function fun) {
        mFun = fun;
        mTempVarCount = 0;
        mCFG = buildCFG(fun);
    }
    
    public Function getFunction() {
        return mFun;
    }
    
    // TODO: Rewrite the function with Java 8 Stream interfaces
    private CFG buildCFG(Function fun) {
        CFG asmCFG = fun.getCFG();
        List<BasicBlock> bbs = new ArrayList<BasicBlock>(asmCFG.getSize());
        Map<BasicBlock, BasicBlock> map = new HashMap<BasicBlock, BasicBlock>();
        for (BasicBlock bb : asmCFG) {
            List<Stmt> irstmt = new ArrayList<Stmt>();
            for (Entry e : bb) {
                Instruction inst = (Instruction)e;
                irstmt.addAll(Stmt.toIRStatements(inst));
            }
            BasicBlock irbb = new BasicBlock(this, irstmt, bb.getLabel());
            bbs.add(irbb);
            map.put(bb, irbb);
        }
        for (BasicBlock bb : asmCFG) {
            BasicBlock irbb = map.get(bb);
            for (BasicBlock pred : bb.getPredecessorAll()) {
                irbb.addPredecessor(map.get(pred));
            }
            for (BasicBlock succ : bb.getSuccessorAll()) {
                irbb.addSuccessor(map.get(succ));
            }
        }
        return createCFGObject(bbs, map.get(asmCFG.getEntryBlock()));
    }

    @Override
    public String getName() {
        return mFun.getName();
    }
    
    @Override
    public CFG getCFG() {
        return mCFG;
    }
    
    public Var getNewTempVariable() {
        return new Var(this, "var_" + mTempVarCount++);
    }
    
    public Var getNewVariable(String name) {
        return new Var(this, name);
    }

    @Override
    public void setEntries(List<? extends Entry> entries) {
        // TODO Auto-generated method stub
    }

}

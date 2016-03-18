package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Procedure;
import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.x86.CompareInst;
import edu.psu.ist.plato.kaiming.x86.Function;
import edu.psu.ist.plato.kaiming.x86.Instruction;
import edu.psu.ist.plato.kaiming.x86.Operand;

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
            List<Stmt> irstmt = new LinkedList<Stmt>();
            for (Entry e : bb) {
                Instruction inst = (Instruction)e;
                irstmt.addAll(toIRStatements(inst));
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
    
    public List<Stmt> toIRStatements(Instruction inst) {
        LinkedList<Stmt> ret = new LinkedList<Stmt>();
        if (inst.isCompareInst())
            toIR((CompareInst)inst, ret);
        else {
            Assert.test(false, "Unreachable code");
        }
        return ret;
    }
    
    private void toIR(CompareInst inst, List<Stmt> ret) {
        Expr e0 = readOperand(inst, 0, ret);
        Expr e1 = readOperand(inst, 1, ret);
        ret.add(new CmpStmt(inst, e0, e1));
    }
    
    private Expr readOperand(Instruction inst, int operandIndex, List<Stmt> stmt) {
        Operand o = inst.getOperand(operandIndex);
        Expr e = null;
        if (o.isImmeidate()) {
            e = Expr.toExpr(o.asImmediate());
        } else if (o.isRegister()) {
            e = Expr.toExpr(o.asRegister());
        } else { // must be a memory-type operand
            Var temp = getNewTempVariable();
            LdStmt load = new LdStmt(inst, Expr.toExpr(o.asMemory()), temp);
            stmt.add(load);
            e = temp;
        }
        return e;
    }

}

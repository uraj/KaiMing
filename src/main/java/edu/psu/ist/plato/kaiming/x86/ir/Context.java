package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Label;
import edu.psu.ist.plato.kaiming.Procedure;
import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.util.Tuple;
import edu.psu.ist.plato.kaiming.x86.BinaryArithInst;
import edu.psu.ist.plato.kaiming.x86.BranchInst;
import edu.psu.ist.plato.kaiming.x86.CallInst;
import edu.psu.ist.plato.kaiming.x86.CompareInst;
import edu.psu.ist.plato.kaiming.x86.DivideInst;
import edu.psu.ist.plato.kaiming.x86.Function;
import edu.psu.ist.plato.kaiming.x86.Instruction;
import edu.psu.ist.plato.kaiming.x86.JumpInst;
import edu.psu.ist.plato.kaiming.x86.Memory;
import edu.psu.ist.plato.kaiming.x86.MoveInst;
import edu.psu.ist.plato.kaiming.x86.Operand;
import edu.psu.ist.plato.kaiming.x86.PopInst;
import edu.psu.ist.plato.kaiming.x86.PushInst;
import edu.psu.ist.plato.kaiming.x86.Register;

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
    
    private Expr readOperand(Instruction inst, int operandIndex, List<Stmt> stmt) {
    	Operand o = inst.getOperand(operandIndex);
    	return readOperand(inst, o, stmt);
    }
    
    private Expr readOperand(Instruction inst, Operand o, List<Stmt> stmt) {    	
        
        Expr e = null;
        if (o.isImmeidate()) {
            e = Expr.toExpr(o.asImmediate());
        } else if (o.isRegister()) {
            e = Expr.toExpr(o.asRegister());
        } else if (o.isMemory()){
            Tuple<Expr, LdStmt> tuple = loadMemory(inst, o.asMemory());
            stmt.add(tuple.second);
            e = tuple.first;
        } else {
            Assert.unreachable();
        }
        return e;
    }
    
    private Tuple<Expr, LdStmt> loadMemory(Instruction inst, Memory mem) {
        Var temp = getNewTempVariable();
        LdStmt load = new LdStmt(inst, Expr.toExpr(mem), temp);
        return new Tuple<Expr, LdStmt>(temp, load);
    }
    
    private void toIR(CompareInst inst, List<Stmt> ret) {
        Expr e0 = readOperand(inst, 0, ret);
        Expr e1 = readOperand(inst, 1, ret);
        ret.add(new CmpStmt(inst, e0, e1));
    }
    
    private void toIR(BranchInst inst, List<Stmt> ret) {
        Expr target = null; 
        if (inst.isIndirect()) {
            Tuple<Expr, LdStmt> tuple = loadMemory(inst, inst.getTarget());
            target = tuple.first;
            ret.add(tuple.second);
        } else {
            target = Expr.toExpr(inst.getTarget());
        }
        Stmt branch = null;
        if (inst.isCallInst()) {
        	branch = new CallStmt((CallInst)inst, target);
        } else if (inst.isJumpInst()) {
        	branch = new JmpStmt((JumpInst)inst, target);
        } else {
        	Assert.unreachable();
        }
        ret.add(branch);
    }
    
    private void toIR(MoveInst inst, List<Stmt> ret) {
    	Operand src = inst.getFrom();
    	Operand dest = inst.getTo();
    	Expr srcResult = readOperand(inst, src, ret);
    	
    	if (dest.isRegister()) {
    		Lval destLval = Reg.getReg(dest.asRegister());
    		ret.add(new AssignStmt(inst, destLval, srcResult));
    	} else if (dest.isMemory()) {
    		ret.add(new StStmt(inst, Expr.toExpr(dest.asMemory()), srcResult));	
    	} else {
    		Assert.unreachable();
    	}
    }
    
    private void toIR(PopInst inst, List<Stmt> ret) {
    	LdStmt load = new LdStmt(inst, Reg.getReg(inst.getTarget()), Reg.esp); 
    	ret.add(load);
    	
    	Const size = Const.getConstant(inst.getOperandSizeInBytes());
    	BExpr incEsp = new BExpr(BExpr.Op.UADD, Reg.esp, size);
    	ret.add(new AssignStmt(inst, Reg.esp, incEsp));
    }
    
    private void toIR(PushInst inst, List<Stmt> ret) {
    	Const size = Const.getConstant(inst.getOperandSizeInBytes());
    	BExpr decEsp = new BExpr(BExpr.Op.USUB, Reg.esp, size);
    	ret.add(new AssignStmt(inst, Reg.esp, decEsp));
    	
    	Expr toPush = null;
    	Operand op = inst.getOperand();
    	if (op.isImmeidate()) {
    		toPush = Expr.toExpr(op.asImmediate());
    	} else if (op.isRegister()) {
    		toPush = Expr.toExpr(op.asRegister());
    	} else {
    		Assert.unreachable();
    	}
    	
    	ret.add(new StStmt(inst, Reg.esp, toPush));
    }
    
    private void toIR(DivideInst inst, List<Stmt> ret) {
    	Tuple<Register, Register> x = inst.getDividend();
    	Expr dividend = new BExpr(BExpr.Op.CONCAT, 
    			Reg.getReg(x.first), Reg.getReg(x.second));
    	Expr divider = readOperand(inst, inst.getDivider(), ret);
    	Expr divid = new BExpr(BExpr.Op.DIV, dividend, divider);
    	Expr low = new UExpr(UExpr.Op.LOW, divid);
    	Expr high = new UExpr(UExpr.Op.HIGH, divid);
    	Tuple<Register, Register> y = inst.getDest();
    	ret.add(new AssignStmt(inst, Reg.getReg(y.first), low));
    	ret.add(new AssignStmt(inst, Reg.getReg(y.second), high));
    }
    
    private void toIR(BinaryArithInst inst, List<Stmt> ret) {
    	
    }
    
    public List<Stmt> toIRStatements(Instruction inst) {
        LinkedList<Stmt> ret = new LinkedList<Stmt>();
        if (inst.isCompareInst())   toIR((CompareInst)inst, ret);
        else if (inst.isJumpInst()) toIR((JumpInst)inst, ret);
        else if (inst.isMoveInst()) toIR((MoveInst)inst, ret);
        else if (inst.isPopInst())  toIR((PopInst)inst, ret);
        else if (inst.isPushInst()) toIR((PushInst)inst, ret);
        else if (inst.isDivideInst()) toIR((DivideInst)inst, ret);
        else {
            Assert.test(false, "Unreachable code");
        }
        return ret;
    }

	@Override
	public Label deriveSubLabel(BasicBlock bb) {
		return null;
	}
}

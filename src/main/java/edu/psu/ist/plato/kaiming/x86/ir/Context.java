package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.psu.ist.plato.kaiming.*;
import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.util.Tuple;
import edu.psu.ist.plato.kaiming.x86.*;

public class Context extends Procedure<Stmt> {

    private Function mFun;
    private CFG<Stmt> mCFG;
    private int mTempVarCount;
    
    public Context(Function fun) {
        mFun = fun;
        mTempVarCount = 0;
        mCFG = buildCFG(fun);
    }
    
    public Function function() {
        return mFun;
    }
    
    @Override
    protected CFG<Stmt> buildCFG(List<Stmt> entries) {
    	throw new UnsupportedOperationException();
    }
    
    private CFG<Stmt> buildCFG(Function fun) {
        CFG<Instruction> asmCFG = fun.cfg();
        List<BasicBlock<Stmt>> bbs = 
                new ArrayList<BasicBlock<Stmt>>(asmCFG.size());
        Map<BasicBlock<Instruction>, BasicBlock<Stmt>> map =
                new HashMap<BasicBlock<Instruction>, BasicBlock<Stmt>>();
        int labelNo = 0;
        for (BasicBlock<Instruction> bb : asmCFG) {
            List<Stmt> irstmt = new LinkedList<Stmt>();
            bb.forEach(inst -> irstmt.addAll(toIRStatements(inst)));
            BasicBlock<Stmt> irbb = 
                    new BasicBlock<Stmt>(this, irstmt, new Label("L_" + labelNo++, -1));
            bbs.add(irbb);
            map.put(bb, irbb);
        }
        for (BasicBlock<Instruction> bb : asmCFG) {
            BasicBlock<Stmt> irbb = map.get(bb);
            bb.allPredecessor().forEach(pred -> irbb.addPredecessor(map.get(pred)));
            bb.allSuccessor().forEach(succ -> irbb.addSuccessor(map.get(succ)));
        }
        // Set indices for IR statements
        int stmtNo = 0;
        for (BasicBlock<Instruction> bb : asmCFG) {
            BasicBlock<Stmt> irbb = map.get(bb);
            for (Stmt s : irbb) {
                Assert.test(s != null);
                s.setIndex(stmtNo++);
            }
            irbb.label().setAddr(irbb.index());
        }
        
        for (BasicBlock<Stmt> bb : bbs) {
            Stmt last = bb.lastEntry();
            if (last.kind() == Stmt.Kind.JMP) {
                JmpStmt js = (JmpStmt)last;
                BranchInst lastInst = (BranchInst)js.hostInstruction();
                if (lastInst.target().isRelocation()) {
                    BasicBlock<Instruction> asmTarget =
                            lastInst.target().asRelocation().targetBlock();
                    js.resolveTarget(map.get(asmTarget));
                }
            }
        }
        return createCFGObject(bbs, map.get(asmCFG.entryBlock()));
    }

    @Override
    public String name() {
        return mFun.name();
    }
    
    @Override
    public CFG<Stmt> cfg() {
        return mCFG;
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
    
    private Stmt updateOperand(Instruction inst, Operand operand, Expr value) {
        Assert.test(!operand.isImmeidate());
        if (operand.isRegister()) {
            return new AssignStmt(inst, Reg.getReg(operand.asRegister()), value);
        } else {
            return new StStmt(inst, getNewTempVariable(), value);
        }
    }
    
    private Stmt updateLval(Instruction inst, Lval lv, Expr value) {
        return new AssignStmt(inst, lv, value);
    }
    
    private Expr readOperand(Instruction inst, int operandIndex, List<Stmt> stmt) {
    	Operand o = inst.operand(operandIndex);
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
            Tuple<Expr, LdStmt> tuple = loadMemory(inst, inst.target());
            target = tuple.first;
            ret.add(tuple.second);
        } else {
            target = Expr.toExpr(inst.target());
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
        // TODO: handle conditional moves
        Assert.test(!inst.isConditional());
    	Operand src = inst.from();
    	Operand dest = inst.to();
    	ret.add(updateOperand(inst, dest, readOperand(inst, src, ret)));
    }
    
    private void toIR(MoveStrInst inst, List<Stmt> ret) {
        Memory src = inst.fromAddr();
        Memory dest = inst.toAddr();
        Var var = getNewTempVariable(inst.moveSizeInBits());
        ret.add(updateLval(inst, var, readOperand(inst, src, ret)));
        ret.add(updateOperand(inst, dest, var));
    }
    
    private void toIR(PopInst inst, List<Stmt> ret) {
    	LdStmt load = new LdStmt(inst, Reg.getReg(inst.popTarget()), Reg.esp); 
    	ret.add(load);
    	
    	Const size = Const.getConstant(inst.sizeInBits() / 8);
    	BExpr incEsp = new BExpr(BExpr.Op.ADD, Reg.esp, size);
    	ret.add(new AssignStmt(inst, Reg.esp, incEsp));
    }
    
    private void toIR(PushInst inst, List<Stmt> ret) {
    	Const size = Const.getConstant(inst.sizeInBits() / 8);
    	BExpr decEsp = new BExpr(BExpr.Op.SUB, Reg.esp, size);
    	ret.add(new AssignStmt(inst, Reg.esp, decEsp));
    	
    	Expr toPush = null;
    	Operand op = inst.pushedOperand();
    	if (op.isImmeidate()) {
    		toPush = Expr.toExpr(op.asImmediate());
    	} else if (op.isRegister()) {
    		toPush = Expr.toExpr(op.asRegister());
    	} else if (op.isMemory()) {
    	    Var var = getNewTempVariable(inst.sizeInBits());
    	    ret.add(updateLval(inst, var, readOperand(inst, op, ret)));
    	    toPush = var;
    	} else {
    		Assert.unreachable();
    	}
    	
    	ret.add(new StStmt(inst, Reg.esp, toPush));
    }
    
    private void toIR(DivideInst inst, List<Stmt> ret) {
    	Tuple<Register, Register> x = inst.dividend();
    	Expr dividend = new BExpr(BExpr.Op.CONCAT, 
    			Reg.getReg(x.first), Reg.getReg(x.second));
    	Expr divider = readOperand(inst, inst.divider(), ret);
    	Expr divid = new BExpr(BExpr.Op.DIV, dividend, divider);
    	Expr low = new UExpr(UExpr.Op.LOW, divid);
    	Expr high = new UExpr(UExpr.Op.HIGH, divid);
    	Tuple<Register, Register> y = inst.dest();
    	ret.add(new AssignStmt(inst, Reg.getReg(y.first), low));
    	ret.add(new AssignStmt(inst, Reg.getReg(y.second), high));
    }
    
    private void toIR(ExchangeInst inst, List<Stmt> ret) {
        Tuple<Operand, Operand> values = inst.exchangedOperands();
        Var d1 = getNewTempVariable();
        Var d2 = getNewTempVariable();
        ret.add(new AssignStmt(inst, d1, readOperand(inst, values.first, ret)));
        ret.add(new AssignStmt(inst, d2, readOperand(inst, values.second, ret)));
        ret.add(updateOperand(inst, values.first, d2));
        ret.add(updateOperand(inst, values.second, d1));
    }
    
    private void toIR(LeaInst inst, List<Stmt> ret) {
        ret.add(new AssignStmt(inst,
                Reg.getReg(inst.loadedRegister()),
                Expr.toExpr(inst.addrExpression())));
    }
    
    private void toIR(UnaryArithInst inst, List<Stmt> ret) {
        UExpr.Op op = null;
        Operand o = inst.operand();
        switch(inst.opcode().opcodeClass()) {
            case INC: // Use a binary expression to hold this
                ret.add(updateOperand(inst, o,
                        new BExpr(BExpr.Op.ADD,
                                readOperand(inst, o, ret),
                                Const.getConstant(1))));
                return;
            case DEC:
                ret.add(updateOperand(inst, o,
                        new BExpr(BExpr.Op.SUB,
                                readOperand(inst, o, ret),
                                Const.getConstant(1))));
                return;
            case NEG:
                ret.add(updateOperand(inst, o,
                        new BExpr(BExpr.Op.SUB,
                                Const.getConstant(0),
                                readOperand(inst, o, ret))));
                return;
            case NOT:
                op = UExpr.Op.NOT;
                break;
            case BSWAP:
                break;
            default:
                Assert.unreachable();
        }
        Expr e = readOperand(inst, o, ret);
        ret.add(updateOperand(inst, o, new UExpr(op, e)));
    }

    private void toIR(BinaryArithInst inst, List<Stmt> ret) {
        BExpr.Op op = null;
        Expr e1 = readOperand(inst, inst.src(), ret);
        Expr e2 = readOperand(inst, inst.dest(), ret);
        switch(inst.opcode().opcodeClass()) {
            case ADD:
            case ADC:
                op = BExpr.Op.ADD;
                break;
            case SUB:
            case SBB:
                op = BExpr.Op.SUB;
                break;
            case AND:
                op = BExpr.Op.AND;
                break;
            case XOR:
                op = BExpr.Op.XOR;
                break;
            case OR:
                op = BExpr.Op.OR;
                break;
            case SAR:
                op = BExpr.Op.SAR;
                break;
            case SHL:
                op = BExpr.Op.SHL;
                break;
            case SHR:
                op = BExpr.Op.SHR;
                break;
            default:
                Assert.unreachable();
        }
        
        // FIXME: The order of e1 and e2 here is critical. Make sure
        // the current ordering reflects the correct semantics of all
        // instructions translated here.
        BExpr bexp = new BExpr(op, e2, e1);
        Operand dest = inst.dest();
        ret.add(updateOperand(inst, dest, bexp));
    }
    
    private void toIR(MultiplyInst inst, List<Stmt> ret) {
        Tuple<Operand, Operand> src = inst.src();
        Expr e1 = readOperand(inst, src.first, ret);
        Expr e2 = readOperand(inst, src.second, ret);
        BExpr bexp = new BExpr(BExpr.Op.MUL, e1, e2);
        Tuple<Operand, Operand> dest = inst.dest();
        Var result = getNewTempVariable(64);
        ret.add(new AssignStmt(inst, result, bexp));
        Expr low = new UExpr(UExpr.Op.LOW, result);
        ret.add(updateOperand(inst, dest.first, low));
        if (dest.second != null) {
            Expr high = new UExpr(UExpr.Op.HIGH, result);
            ret.add(updateOperand(inst, dest.second, high));
        }
    }
    
    public List<Stmt> toIRStatements(Instruction inst) {
        LinkedList<Stmt> ret = new LinkedList<Stmt>();
        switch (inst.kind()) {
            case BIN_ARITH:
                toIR((BinaryArithInst)inst, ret);
                break;
            case UN_ARITH:
                toIR((UnaryArithInst)inst, ret);
                break;
            case CALL:
                CallInst call = (CallInst)inst;
                ret.add(new CallStmt(call, Expr.toExpr(call.target())));
                break;
            case COMPARE:
                toIR((CompareInst)inst, ret);
                break;
            case DIVIDE:
                toIR((DivideInst)inst, ret);
                break;
            case EXCHANGE:
                toIR((ExchangeInst)inst, ret);
                break;
            case JUMP:
                toIR((JumpInst)inst, ret);
                break;
            case LEA:
                toIR((LeaInst)inst, ret);
                break;
            case MOVE:
                toIR((MoveInst)inst, ret);
                break;
            case MOVE_STR:
                toIR((MoveStrInst)inst, ret);
                break;
            case MULTIPLY:
                toIR((MultiplyInst)inst, ret);
                break;
            case POP:
                toIR((PopInst)inst, ret);
                break;
            case PUSH:
                toIR((PushInst)inst, ret);
                break;
            case RETURN:
                ret.add(new RetStmt(inst));
                break;
            case NOP:
                break;
            case COND_SET:
            case BIT_TEST:
            case OTHER:
                Assert.unreachable("Unrecogized instruction: " + inst);
        }
        return ret;
    }

	@Override
	protected Label deriveSubLabel(BasicBlock<Stmt> bb) {
		return null;
	}
}

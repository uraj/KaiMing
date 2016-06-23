package edu.psu.ist.plato.kaiming.x86;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Label;
import edu.psu.ist.plato.kaiming.Machine;
import edu.psu.ist.plato.kaiming.ir.*;
import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.util.Tuple;

public class X86Machine extends Machine {
    
    public static final X86Machine instance = new X86Machine();
    
    private static Reg esp = Reg.getReg(Register.getRegister("esp"));
    private static Reg ebp = Reg.getReg(Register.getRegister("ebp"));
    
    private X86Machine() {
        super(Arch.X86);
    }
    
    private static Const toExpr(Immediate imm) {
        return Const.getConstant(imm.getValue());
    }
    
    private static Expr toExpr(Register reg) {
        return Reg.getReg(reg);
    }
    
    private static Expr toExpr(Memory mem) {
        Expr ret = null;
        if (mem.offsetRegister() != null) {
            ret = toExpr(mem.offsetRegister());
            if (mem.multiplier() != 1) {
                ret = new BExpr(BExpr.Op.MUL, ret, Const.getConstant(mem.multiplier()));
            }
        }
        if (mem.baseRegister() != null) {
            if (ret == null) {
                ret = toExpr(mem.baseRegister());
            } else {
                ret = new BExpr(BExpr.Op.ADD, toExpr(mem.baseRegister()), ret);
            }
        }
        if (mem.displacement() != 0) {
            if (ret == null) {
                ret = Const.getConstant(mem.displacement());
            } else {
                ret = new BExpr(BExpr.Op.ADD, Const.getConstant(mem.displacement()), ret);
            }
        }
        if (ret == null)
            ret = Const.getConstant(mem.displacement());
        return ret;
    }
    
    private Stmt updateOperand(Instruction inst, Operand operand, Expr value) {
        if (operand.isRegister()) {
            return new AssignStmt(inst, Reg.getReg(operand.asRegister()), value);
        } else if (operand.isMemory()){
            Expr addr = toExpr(operand.asMemory());
            return new StStmt(inst, addr, value);
        } else {
            Assert.unreachable();
            return null;
        }
        
    }
    
    private Stmt updateLval(Instruction inst, Lval lv, Expr value) {
        return new AssignStmt(inst, lv, value);
    }
    
    private Expr readOperand(Context ctx, Instruction inst, int operandIndex, List<Stmt> stmt) {
        Operand o = inst.operand(operandIndex);
        return readOperand(ctx, inst, o, stmt);
    }
    
    private Expr readOperand(Context ctx, Instruction inst, Operand o, List<Stmt> stmt) {        
        
        Expr e = null;
        if (o.isImmeidate()) {
            e = toExpr(o.asImmediate());
        } else if (o.isRegister()) {
            e = toExpr(o.asRegister());
        } else if (o.isMemory()){
            Tuple<Expr, LdStmt> tuple = loadMemory(ctx, inst, o.asMemory());
            stmt.add(tuple.second);
            e = tuple.first;
        } else {
            Assert.unreachable();
        }
        return e;
    }
    
    private Tuple<Expr, LdStmt> loadMemory(Context ctx, Instruction inst, Memory mem) {
        Var temp = ctx.getNewTempVariable();
        LdStmt load = new LdStmt(inst, temp, toExpr(mem));
        return new Tuple<Expr, LdStmt>(temp, load);
    }
    
    private void toIR(Context ctx, CompareInst inst, List<Stmt> ret) {
        Expr e0 = readOperand(ctx, inst, 0, ret);
        Expr e1 = readOperand(ctx, inst, 1, ret);
        ret.add(new CmpStmt(inst, e0, e1));
    }
    
    private void toIR(Context ctx, BranchInst inst, List<Stmt> ret) {
        Expr target = null; 
        if (inst.isIndirect()) {
            Tuple<Expr, LdStmt> tuple = loadMemory(ctx, inst, inst.target());
            target = tuple.first;
            ret.add(tuple.second);
        } else {
            target = toExpr(inst.target());
        }
        Stmt branch = null;
        if (inst.isCallInst()) {
            branch = new CallStmt((CallInst)inst, target);
        } else if (inst.isJumpInst()) {
            JumpInst j = (JumpInst)inst;
            branch = new JmpStmt(j, target, 
                    j.dependentFlags().stream().map(x -> Flg.getFlg(x)).collect(Collectors.toSet()));
        } else {
            Assert.unreachable();
        }
        ret.add(branch);
    }
    
    private void toIR(Context ctx, MoveInst inst, List<Stmt> ret) {
        // TODO: handle conditional moves
        Assert.test(!inst.isConditional());
        Operand src = inst.from();
        Operand dest = inst.to();
        ret.add(updateOperand(inst, dest, readOperand(ctx, inst, src, ret)));
    }
    
    private void toIR(Context ctx, MoveStrInst inst, List<Stmt> ret) {
        Memory src = inst.fromAddr();
        Memory dest = inst.toAddr();
        Var var = ctx.getNewTempVariable(inst.moveSizeInBits());
        ret.add(updateLval(inst, var, readOperand(ctx, inst, src, ret)));
        ret.add(updateOperand(inst, dest, var));
    }
    
    private void toIR(PopInst inst, List<Stmt> ret) {
        LdStmt load = new LdStmt(inst, Reg.getReg(inst.popTarget()), esp); 
        ret.add(load);
        
        Const size = Const.getConstant(inst.sizeInBits() / 8);
        BExpr incEsp = new BExpr(BExpr.Op.ADD, esp, size);
        ret.add(new AssignStmt(inst, esp, incEsp));
    }
    
    private void toIR(Context ctx, PushInst inst, List<Stmt> ret) {
        Const size = Const.getConstant(inst.sizeInBits() / 8);
        BExpr decEsp = new BExpr(BExpr.Op.SUB, esp, size);
        ret.add(new AssignStmt(inst, esp, decEsp));
        
        Expr toPush = null;
        Operand op = inst.pushedOperand();
        if (op.isImmeidate()) {
            toPush = toExpr(op.asImmediate());
        } else if (op.isRegister()) {
            toPush = toExpr(op.asRegister());
        } else if (op.isMemory()) {
            Var var = ctx.getNewTempVariable(inst.sizeInBits());
            ret.add(updateLval(inst, var, readOperand(ctx, inst, op, ret)));
            toPush = var;
        } else {
            Assert.unreachable();
        }
        
        ret.add(new StStmt(inst, esp, toPush));
    }
    
    private void toIR(Context ctx, DivideInst inst, List<Stmt> ret) {
        Tuple<Register, Register> x = inst.dividend();
        Expr dividend = new BExpr(BExpr.Op.CONCAT, 
                Reg.getReg(x.first), Reg.getReg(x.second));
        Expr divider = readOperand(ctx, inst, inst.divider(), ret);
        Expr divid = new BExpr(BExpr.Op.DIV, dividend, divider);
        Expr low = new UExpr(UExpr.Op.LOW, divid);
        Expr high = new UExpr(UExpr.Op.HIGH, divid);
        Tuple<Register, Register> y = inst.dest();
        ret.add(new AssignStmt(inst, Reg.getReg(y.first), low));
        ret.add(new AssignStmt(inst, Reg.getReg(y.second), high));
    }
    
    private void toIR(Context ctx, ExchangeInst inst, List<Stmt> ret) {
        Tuple<Operand, Operand> values = inst.exchangedOperands();
        Var d1 = ctx.getNewTempVariable();
        Var d2 = ctx.getNewTempVariable();
        ret.add(new AssignStmt(inst, d1, readOperand(ctx, inst, values.first, ret)));
        ret.add(new AssignStmt(inst, d2, readOperand(ctx, inst, values.second, ret)));
        ret.add(updateOperand(inst, values.first, d2));
        ret.add(updateOperand(inst, values.second, d1));
    }
    
    private void toIR(LeaInst inst, List<Stmt> ret) {
        ret.add(new AssignStmt(inst,
                Reg.getReg(inst.loadedRegister()),
                toExpr(inst.addrExpression())));
    }
    
    private void toIR(Context ctx, UnaryArithInst inst, List<Stmt> ret) {
        UExpr.Op op = null;
        Operand o = inst.operand();
        switch(inst.opcode().opcodeClass()) {
            case INC: // Use a binary expression to hold this
                ret.add(updateOperand(inst, o,
                        new BExpr(BExpr.Op.ADD,
                                readOperand(ctx, inst, o, ret),
                                Const.getConstant(1))));
                return;
            case DEC:
                ret.add(updateOperand(inst, o,
                        new BExpr(BExpr.Op.SUB,
                                readOperand(ctx, inst, o, ret),
                                Const.getConstant(1))));
                return;
            case NEG:
                ret.add(updateOperand(inst, o,
                        new BExpr(BExpr.Op.SUB,
                                Const.getConstant(0),
                                readOperand(ctx, inst, o, ret))));
                return;
            case NOT:
                op = UExpr.Op.NOT;
                break;
            case BSWAP:
                break;
            default:
                Assert.unreachable();
        }
        Expr e = readOperand(ctx, inst, o, ret);
        ret.add(updateOperand(inst, o, new UExpr(op, e)));
    }

    private void toIR(Context ctx, BinaryArithInst inst, List<Stmt> ret) {
        BExpr.Op op = null;
        Expr e1 = readOperand(ctx, inst, inst.src(), ret);
        Expr e2 = readOperand(ctx, inst, inst.dest(), ret);
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
    
    private void toIR(LeaveInst inst, List<Stmt> ret) {
        ret.add(new AssignStmt(inst, esp, ebp));
        LdStmt load = new LdStmt(inst, ebp, esp); 
        ret.add(load);
        Const size = Const.getConstant(4);
        BExpr incEsp = new BExpr(BExpr.Op.ADD, esp, size);
        ret.add(new AssignStmt(inst, esp, incEsp));
    }
    
    private void toIR(Context ctx, MultiplyInst inst, List<Stmt> ret) {
        Tuple<Operand, Operand> src = inst.src();
        Expr e1 = readOperand(ctx, inst, src.first, ret);
        Expr e2 = readOperand(ctx, inst, src.second, ret);
        BExpr bexp = new BExpr(BExpr.Op.MUL, e1, e2);
        Tuple<Operand, Operand> dest = inst.dest();
        Var result = ctx.getNewTempVariable(64);
        ret.add(new AssignStmt(inst, result, bexp));
        Expr low = new UExpr(UExpr.Op.LOW, result);
        ret.add(updateOperand(inst, dest.first, low));
        if (dest.second != null) {
            Expr high = new UExpr(UExpr.Op.HIGH, result);
            ret.add(updateOperand(inst, dest.second, high));
        }
    }
    
    public List<Stmt> toIRStatements(Context ctx, Instruction inst) {
        LinkedList<Stmt> ret = new LinkedList<Stmt>();
        switch (inst.kind()) {
            case BIN_ARITH:
                toIR(ctx, (BinaryArithInst)inst, ret);
                break;
            case UN_ARITH:
                toIR(ctx, (UnaryArithInst)inst, ret);
                break;
            case CALL:
                CallInst call = (CallInst)inst;
                ret.add(new CallStmt(call, toExpr(call.target())));
                break;
            case COMPARE:
                toIR(ctx, (CompareInst)inst, ret);
                break;
            case DIVIDE:
                toIR(ctx, (DivideInst)inst, ret);
                break;
            case EXCHANGE:
                toIR(ctx, (ExchangeInst)inst, ret);
                break;
            case JUMP:
                toIR(ctx, (JumpInst)inst, ret);
                break;
            case LEA:
                toIR((LeaInst)inst, ret);
                break;
            case MOVE:
                toIR(ctx, (MoveInst)inst, ret);
                break;
            case MOVE_STR:
                toIR(ctx, (MoveStrInst)inst, ret);
                break;
            case MULTIPLY:
                toIR(ctx, (MultiplyInst)inst, ret);
                break;
            case POP:
                toIR((PopInst)inst, ret);
                break;
            case PUSH:
                toIR(ctx, (PushInst)inst, ret);
                break;
            case RETURN:
                ret.add(new RetStmt(inst));
                break;
            case LEAVE:
                toIR((LeaveInst)inst, ret);
            case NOP:
                break;
            case COND_SET:
            case BIT_TEST:
            case OTHER:
                Assert.unreachable("Unrecogized instruction: " + inst);
        }
        return ret;
    }
    
    public Tuple<List<BasicBlock<Stmt>>, BasicBlock<Stmt>>
    buildCFGForX86(Context ctx, Function fun) {
        CFG<Instruction> asmCFG = fun.cfg();
        List<BasicBlock<Stmt>> bbs = 
                new ArrayList<BasicBlock<Stmt>>(asmCFG.size());
        Map<BasicBlock<Instruction>, BasicBlock<Stmt>> map =
                new HashMap<BasicBlock<Instruction>, BasicBlock<Stmt>>();
        int labelNo = 0;
        for (BasicBlock<Instruction> bb : asmCFG) {
            List<Stmt> irstmt = new LinkedList<Stmt>();
            bb.forEach(inst -> irstmt.addAll(toIRStatements(ctx, inst)));
            BasicBlock<Stmt> irbb = 
                    new BasicBlock<Stmt>(ctx, irstmt, new Label("L_" + labelNo++, -1));
            bbs.add(irbb);
            map.put(bb, irbb);
        }
        for (BasicBlock<Instruction> bb : asmCFG) {
            BasicBlock<Stmt> irbb = map.get(bb);
            bb.predecessors().forEach(pred -> irbb.addPredecessor(map.get(pred)));
            bb.successors().forEach(succ -> irbb.addSuccessor(map.get(succ)));
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
                BranchInst lastInst = (BranchInst)js.hostEntry();
                if (lastInst.target().isRelocation()) {
                    BasicBlock<Instruction> asmTarget =
                            lastInst.target().asRelocation().targetBlock();
                    js.resolveTarget(map.get(asmTarget));
                }
            }
        }
        return new Tuple<List<BasicBlock<Stmt>>, BasicBlock<Stmt>>(
                bbs, map.get(asmCFG.entryBlock()));
    }

    @Override
    public List<MachRegister> registers() {
        Register.Id[] allRegs = Register.Id.values();
        List<MachRegister> ret = new ArrayList<MachRegister>();
        for (Register.Id id : allRegs) {
            ret.add(Register.getRegister(id));
        }
        return ret;
    }

    @Override
    public MachRegister returnRegister() {
        return Register.getRegister("eax");
    }

    @Override
    public int wordSizeInBits() {
        return 32;
    }
}

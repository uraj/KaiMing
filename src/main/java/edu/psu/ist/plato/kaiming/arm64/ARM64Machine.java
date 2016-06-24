package edu.psu.ist.plato.kaiming.arm64;

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
import edu.psu.ist.plato.kaiming.MachRegister;
import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.util.Tuple;
import edu.psu.ist.plato.kaiming.ir.*;

public class ARM64Machine extends Machine {

    public static final ARM64Machine instance = new ARM64Machine();
    
    protected ARM64Machine() {
        super(Arch.ARM64);
    }

    @Override
    public List<MachRegister> registers() {
        Register.Id[] allRegs = Register.Id.values();
        List<MachRegister> ret = new ArrayList<MachRegister>();
        for (Register.Id id : allRegs) {
            ret.add(Register.get(id));
        }
        return ret;
    }

    @Override
    public MachRegister returnRegister() {
        return Register.get("X0");
    }

    @Override
    public int wordSizeInBits() {
        return 64;
    }
    
    private Const toExpr(Immediate imm) {
        return Const.get(imm.value());
    }
    
    private Expr toExpr(Register reg) {
        Expr ret = Reg.get(reg);
        if (reg.isShifted()) {
            Register.Shift shift = reg.shift();
            Const off = Const.get(shift.value());
            switch (shift.type()) {
                case ASR:
                    ret = ret.sar(off);
                    break;
                case LSL:
                    ret = ret.shl(off);
                    break;
                case ROR:
                    ret = ret.ror(off);
                    break;
            }
        }
        return ret;
    }
    
    private Expr toExpr(Memory mem) {
        Expr ret = null;
        if (mem.base() != null)
            ret = toExpr(mem.base());
        Memory.Offset off = mem.offset(); 
        if (mem.offset() != null) {
            Expr oe = null;
            if (off.isImmediateOffset())
                oe = Const.get(off.asImmOff().value);
            else {
                oe = toExpr(off.asRegOff().value);
            }
            if (ret == null)
                ret = oe;
            else
                ret = ret.add(oe);
        }
        return ret;
    }
    
    private Expr operandToExpr(Operand op) {
        Expr ret = null;
        switch (op.type()) {
            case IMMEDIATE:
                ret = toExpr(op.asImmediate());
                break;
            case MEMORY:
                ret = toExpr(op.asMemory());
                break;
            case REGISTER:
                ret = toExpr(op.asRegister());
                break;
        }
        return ret;
    }
    
    private void toIR(BinaryArithInst inst, List<Stmt> ret) {
        Expr rval = null;
        Lval lval = Reg.get(inst.dest().asRegister());
        Expr op1 = operandToExpr(inst.srcLeft());
        Expr op2 = operandToExpr(inst.srcRight());
        switch (inst.opcode().mnemonic()) {
            case ADD:
                rval = op1.add(op2);
                break;
            case SUB:
                rval = op1.add(op2);
                break;
            case MUL:
                rval = op1.mul(op2);
                break;
            case DIV:
                rval = op1.div(op2);
                break;
            case ASR:
                rval = op1.sar(op2);
                break;
            case LSL:
                rval = op1.shl(op2);
                break;
            case LSR:
                rval = op1.shr(op2);
                break;
            case ORR:
                rval = op1.or(op2);
                break;
            case ORN:
                rval = op1.or(op2).not();
                break;
            case AND:
                rval = op1.and(op2);
                break;
            default:
                Assert.unreachable();
        }
        ret.add(new AssignStmt(inst, lval, rval));
    }
    
    private void toIR(Context ctx, BitfieldMoveInst inst, List<Stmt> ret) {
        Lval lval = Reg.get(inst.dest());
        if (inst instanceof ExtensionInst) {
            BExpr e = null;
            if (inst.extension() == BitfieldMoveInst.Extension.SIGNED)
                e = lval.sext(Const.get(lval.sizeInBits()));
            else
                e = lval.uext(Const.get(lval.sizeInBits()));
           ret.add(new AssignStmt(inst, lval, e));
        } else {
            int rotate = (int)inst.rotate().value(), shift = (int)inst.shift().value();
            int destInsig, destSig, srcInsig, srcSig;
            int size = lval.sizeInBits();
            if (shift >= rotate) {
                destInsig = 0;
                destSig = shift - rotate;
                srcInsig = rotate;
                srcSig = shift;
            } else {
                destInsig = size - rotate;
                destSig = size + shift - rotate;
                srcInsig = 0;
                srcSig = shift;
            }
            Expr assigned = toExpr(inst.src()); 
            if (srcInsig > 0) {
                assigned = assigned.shr(Const.get(srcInsig));
            }
            Lval tmp = ctx.getNewTempVariable(srcSig - srcInsig + 1);
            ret.add(new AssignStmt(inst, tmp, assigned)); // This triggers a truncate
            assigned = tmp;
            if (srcInsig > 0) { // Paddings required on the right
                tmp = ctx.getNewTempVariable(srcInsig);
                ret.add(new AssignStmt(inst, tmp, Const.get(0)));
                assigned = assigned.concat(tmp);
            }
            int assignRangeSig = size, assignRangeInsig = 0;
            switch (inst.extension()) {
                case SIGNED:
                    assigned = assigned.sext(Const.get(size));
                    break;
                case UNSIGNED:
                    assigned = assigned.uext(Const.get(size));
                    break;
                case NIL: // There will be a partial assignment
                    assignRangeSig = destSig;
                    assignRangeInsig = destInsig;
                    break;
            }
            ret.add(new AssignStmt(inst, lval, assigned, assignRangeInsig, assignRangeSig));
        }
    }
    
    private void toIR(MoveInst inst, List<Stmt> ret) {
        Lval lval = Reg.get(inst.dest());
        Expr rval = operandToExpr(inst.src());
        int boundSig = lval.sizeInBits();
        if (inst.keep()) {
            boundSig = 16;
        }
        ret.add(new AssignStmt(inst, lval, rval, 0, boundSig));
    }
    
    private void toIR(CompareInst inst, List<Stmt> ret) {
        Expr cmp1 = toExpr(inst.comparedLeft());
        Expr cmp2 = operandToExpr(inst.comparedRight());
        Expr cmp =  inst.isTest() ? cmp1.and(cmp2) : cmp1.sub(cmp2);
                
        Flg c = Flg.get(Flag.C);
        ret.add(new AssignStmt(inst, c, cmp.fcarry()));
        Flg n = Flg.get(Flag.N);
        ret.add(new AssignStmt(inst, n, cmp.fnegative()));
        Flg z = Flg.get(Flag.Z);
        ret.add(new AssignStmt(inst, z, cmp.fzero()));
        Flg v = Flg.get(Flag.V);
        ret.add(new AssignStmt(inst, v, cmp.foverflow()));
    }
    
    private void toIR(BranchInst inst, List<Stmt> ret) {
        if (inst.isReturn()) {
            ret.add(new RetStmt(inst, operandToExpr(inst.target())));
        } else if (inst.isCall()) {
            ret.add(new CallStmt(inst, operandToExpr(inst.target())));
        } else {
            ret.add(new JmpStmt(inst, operandToExpr(inst.target()),
                    inst.dependentFlags().stream().map(
                            x -> Flg.get(x)).collect(Collectors.toSet())));
        }
    }
    
    public void toIR(PopInst inst, List<Stmt> ret) {
        int n = inst.numOfPoppedRegisters();
        Reg sp = Reg.get(Register.get(Register.Id.SP));
        
        if (n == 1) {
            Reg popped = Reg.get(inst.firstPoppedRegister());
            ret.add(new LdStmt(inst, popped, sp));
            Expr add = sp.add(Const.get(popped.sizeInBits() / 8));
            ret.add(new AssignStmt(inst, sp, add));
        } else {
            for (Register p : inst.poppedRegisters()) {
                Reg popped = Reg.get(p);
                ret.add(new LdStmt(inst, popped, sp));
                Expr add = sp.add(Const.get(popped.sizeInBits() / 8));
                ret.add(new AssignStmt(inst, sp, add));
            }
        }
    }
    
    public void toIR(PushInst inst, List<Stmt> ret) {
        int n = inst.numOfPushedRegisters();
        Reg sp = Reg.get(Register.get(Register.Id.SP));
        
        if (n == 1) {
            Reg popped = Reg.get(inst.firstPushedRegister());
            ret.add(new LdStmt(inst, popped, sp));
            Expr sub = sp.sub(Const.get(popped.sizeInBits() / 8));
            ret.add(new AssignStmt(inst, sp, sub));
        } else {
            for (Register p : inst.pushedRegisters()) {
                Reg popped = Reg.get(p);
                ret.add(new LdStmt(inst, popped, sp));
                Expr sub = sp.sub(Const.get(popped.sizeInBits() / 8));
                ret.add(new AssignStmt(inst, sp, sub));
            }
        }
    }
    
    private void toIR(Context ctx, LoadStoreInst inst, List<Stmt> ret) {
        LoadStoreInst.AddressingMode mode = inst.addressingMode();
        Memory mem = inst.indexingOperand();
        Register base = mem.base();
        Expr addr = null;
        switch (mode) {
            case POST_INDEX:
                addr = toExpr(mem.base());
                break;
            case PRE_INDEX:
            case REGULAR:
                addr = toExpr(mem);
                break;
        }
        if (!addr.isPrimitive()) {
            Var tmp = ctx.getNewTempVariable();
            ret.add(new AssignStmt(inst, tmp, addr));
            addr = tmp;
        }
        
        if (inst instanceof LoadInst)
            processLoadStore((LoadInst)inst, addr, ret);
        else if (inst instanceof LoadPairInst)
            processLoadStore((LoadPairInst)inst, addr, ret);
        else if (inst instanceof StoreInst)
            processLoadStore((StoreInst)inst, addr, ret);
        else if (inst instanceof StorePairInst)
            processLoadStore((StorePairInst)inst, addr, ret);
        else
            Assert.unreachable();
        
        // pre- or post-indexing requires the base register to be updated
        if (mode != LoadStoreInst.AddressingMode.REGULAR) {
            ret.add(new AssignStmt(inst, Reg.get(base), addr));
        }
        
        return;
    }
    
    private void processLoadStore(LoadInst inst, Expr addr, List<Stmt> ret) {
        Lval lval = Reg.get(inst.dest());
        ret.add(new LdStmt(inst, lval, addr));
    }
    
    private void processLoadStore(LoadPairInst inst, Expr addr, List<Stmt> ret) {
        Reg first = Reg.get(inst.destLeft());
        int sizeInBytes = first.sizeInBits() / 8;
        ret.add(new LdStmt(inst, first, addr));
        Reg second = Reg.get(inst.destRight());
        Const disp = Const.get(sizeInBytes);
        ret.add(new LdStmt(inst, second, addr.add(disp)));
    }
    
    private void processLoadStore(StoreInst inst, Expr addr, List<Stmt> ret) {
        Expr e = toExpr(inst.src());
        ret.add(new StStmt(inst, addr, e));
    }
    
    private void processLoadStore(StorePairInst inst, Expr addr, List<Stmt> ret) {
        Reg first = Reg.get(inst.srcLeft());
        int sizeInBytes = first.sizeInBits() / 8;
        ret.add(new LdStmt(inst, first, addr));
        Reg second = Reg.get(inst.srcRight());
        Const disp = Const.get(sizeInBytes);
        ret.add(new LdStmt(inst, second, addr.add(disp)));
    }
    
    private void toIR(UnaryArithInst inst, List<Stmt> ret) {
        Lval lval = Reg.get(inst.dest());
        switch (inst.opcode().mnemonic()) {
            case ADR:
                ret.add(new AssignStmt(inst, lval, operandToExpr(inst.src())));
                break;
            case NEG:
                ret.add(new AssignStmt(inst, lval, 
                		Const.get(0).sub(operandToExpr(inst.src()))));
                break;
            default:
                Assert.unreachable();
        }
    }
    
    private Expr toExpr(Condition cond) {
        Expr ret = null;
        switch (cond) {
            case AL:
                ret = Const.get(1);
                break;
            case EQ:
                // Z == 1
                ret = Flg.get(Flag.Z);
                break;
            case GE:
                // N == V
                ret = Flg.get(Flag.N).sub(Flg.get(Flag.V)).not();
                break;
            case GT:
                // Z == 0 && N == V
                ret = Flg.get(Flag.Z).not().and(
                		Flg.get(Flag.N).sub(Flg.get(Flag.V).not()));
                break;
            case HI:
                // C ==1 && Z == 0
                ret = Flg.get(Flag.C).and(Flg.get(Flag.Z).not());
                break;
            case HS:
                // C == 1
                ret = Flg.get(Flag.C);
                break;
            case LE:
                // Z == 1 || N != V
                ret = Flg.get(Flag.Z).or(
                		Flg.get(Flag.N).sub(Flg.get(Flag.Z)));
                break;
            case LO:
                // C == 0
                ret = Flg.get(Flag.C).not();
                break;
            case LS:
                // C == 0 || Z == 1
                ret = Flg.get(Flag.Z).or(Flg.get(Flag.C).not());
                break;
            case LT:
                // N != V
                ret = Flg.get(Flag.N).sub(Flg.get(Flag.V));
                break;
            case MI:
                // N == 1
                ret = Flg.get(Flag.N);
                break;
            case NE:
                // Z == 0
                ret = Flg.get(Flag.Z).not();
                break;
            case PL:
                // N == 0
                ret = Flg.get(Flag.N).not();
                break;
            case VC:
                // V == 0
                ret = Flg.get(Flag.V).not();
                break;
            case VS:
                // V == 1
                ret = Flg.get(Flag.V);
                break;
            default:
                break;
            
        }
        return ret;
    }
    
    private void toIR(SelectInst inst, List<Stmt> ret) {
        Expr cond = toExpr(inst.condition());
        ret.add(new SelStmt(inst, Reg.get(inst.dest()),
                cond, toExpr(inst.truevalue()), toExpr(inst.falsevalue())));
    }
    
    public List<Stmt> toIRStatements(Context ctx, Instruction inst) {
        LinkedList<Stmt> ret = new LinkedList<Stmt>();
        switch (inst.kind()) {
            case BIN_ARITHN:
                toIR((BinaryArithInst)inst, ret);
                break;
            case BITFIELD_MOVE:
                toIR(ctx, (BitfieldMoveInst)inst, ret);
                break;
            case BRANCH:
                toIR((BranchInst)inst, ret);
                break;
            case COMPARE:
                toIR((CompareInst)inst, ret);
                break;
            case MOVE:
                toIR((MoveInst)inst, ret);
                break;
            case POP:
                toIR((PopInst)inst, ret);
                break;
            case PUSH:
                toIR((PushInst)inst, ret);
                break;
            case SELECT:
                toIR((SelectInst)inst, ret);
                break;
            case LOAD:
            case LOAD_PAIR:
            case STORE:
            case STORE_PAIR:
                toIR(ctx, (LoadStoreInst)inst, ret);
                break;
            case UN_ARITH:
                toIR((UnaryArithInst)inst, ret);
                break;
            case NOP:
                break;
        }
        return ret;
    }

    public Tuple<List<BasicBlock<Stmt>>, BasicBlock<Stmt>>
    buildCFGForARM64(Context ctx, Function fun) {
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
                if (lastInst.target() instanceof Relocation) {
                    BasicBlock<Instruction> asmTarget =
                            ((Relocation)lastInst.target()).targetBlock();
                    js.resolveTarget(map.get(asmTarget));
                }
            }
        }
        return new Tuple<List<BasicBlock<Stmt>>, BasicBlock<Stmt>>(
                bbs, map.get(asmCFG.entryBlock()));
    }
}

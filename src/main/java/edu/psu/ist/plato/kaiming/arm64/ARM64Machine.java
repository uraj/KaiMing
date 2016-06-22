package edu.psu.ist.plato.kaiming.arm64;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import edu.psu.ist.plato.kaiming.Machine;
import edu.psu.ist.plato.kaiming.util.Assert;
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
            ret.add(Register.getRegister(id));
        }
        return null;
    }

    @Override
    public MachRegister returnRegister() {
        return Register.getRegister("R0");
    }

    @Override
    public int wordSizeInBits() {
        return 64;
    }
    
    private Const toExpr(Immediate imm) {
        return Const.getConstant(imm.getValue());
    }
    
    private Expr toExpr(Register reg) {
        Expr ret = Reg.getReg(reg);
        if (reg.isShifted()) {
            Register.Shift shift = reg.shift();
            Const off = Const.getConstant(shift.value());
            switch (shift.type()) {
                case ASR:
                    ret = new BExpr(BExpr.Op.SAR, ret, off);
                    break;
                case LSL:
                    ret = new BExpr(BExpr.Op.SHL, ret, off);
                    break;
                case ROR:
                    ret = new BExpr(BExpr.Op.ROR, ret, off);
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
                oe = Const.getConstant(off.asImmOff().value);
            else {
                oe = toExpr(off.asRegOff().value);
            }
            if (ret == null)
                ret = oe;
            else
                ret = new BExpr(BExpr.Op.ADD, ret, oe);
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
        Lval lval = Reg.getReg(inst.dest().asRegister());
        Expr op1 = operandToExpr(inst.srcLeft());
        Expr op2 = operandToExpr(inst.srcRight());
        switch (inst.opcode().mnemonic()) {
            case ADD:
                rval = new BExpr(BExpr.Op.ADD, op1, op2);
                break;
            case SUB:
                rval = new BExpr(BExpr.Op.SUB, op1, op2);
                break;
            case MUL:
                rval = new BExpr(BExpr.Op.MUL, op1, op2);
                break;
            case DIV:
                rval = new BExpr(BExpr.Op.DIV, op1, op2);
                break;
            case ASR:
                rval = new BExpr(BExpr.Op.SAR, op1, op2);
                break;
            case LSL:
                rval = new BExpr(BExpr.Op.SHL, op1, op2);
                break;
            case LSR:
                rval = new BExpr(BExpr.Op.SHR, op1, op2);
                break;
            case ORR:
                rval = new BExpr(BExpr.Op.OR, op1, op2);
                break;
            case ORN:
                rval = new UExpr(UExpr.Op.NOT, new BExpr(BExpr.Op.OR, op1, op2));
                break;
            case AND:
                rval = new BExpr(BExpr.Op.AND, op1, op2);
                break;
            default:
                Assert.unreachable();
        }
        ret.add(new AssignStmt(inst, lval, rval));
    }
    
    private void toIR(BitfieldMoveInst inst, List<Stmt> ret) {
        Lval lval = Reg.getReg(inst.dest());
        if (inst.isExtension()) {
            BExpr.Op op = inst.isSigned() ? BExpr.Op.SEXT : BExpr.Op.UEXT;
            new AssignStmt(inst, lval, new BExpr(op, lval, Const.getConstant(lval.sizeInBits())));
        } else {
            Assert.unreachable();
        }
    }
    
    private void toIR(MoveInst inst, List<Stmt> ret) {
        Lval lval = Reg.getReg(inst.dest());
        Expr rval = toExpr(inst.dest());
        int boundSig = lval.sizeInBits();
        if (inst.keep()) {
            boundSig = 16;
        }
        ret.add(new AssignStmt(inst, lval, rval, 0, boundSig));
    }
    
    private void toIR(CompareInst inst, List<Stmt> ret) {
        Expr cmp = new BExpr(BExpr.Op.SUB, toExpr(inst.comparedLeft()),
                toExpr(inst.comparedRight()));
        Flg c = Flg.getFlg(Flag.C);
        ret.add(new AssignStmt(inst, c, new UExpr(UExpr.Op.CARRY, cmp)));
        Flg n = Flg.getFlg(Flag.N);
        ret.add(new AssignStmt(inst, n, new UExpr(UExpr.Op.NEGATIVE, cmp)));
        Flg z = Flg.getFlg(Flag.Z);
        ret.add(new AssignStmt(inst, z, new UExpr(UExpr.Op.ZERO, cmp)));
        Flg v = Flg.getFlg(Flag.V);
        ret.add(new AssignStmt(inst, v, new UExpr(UExpr.Op.CARRY, cmp)));
    }
    
    private void toIR(BranchInst inst, List<Stmt> ret) {
        if (inst.isReturn()) {
            ret.add(new RetStmt(inst, operandToExpr(inst.target())));
        } else if (inst.isCall()) {
            ret.add(new CallStmt(inst, operandToExpr(inst.target())));
        } else {
            ret.add(new JmpStmt(inst, operandToExpr(inst.target()),
                    inst.dependentFlags().stream().map(
                            x -> Flg.getFlg(x)).collect(Collectors.toSet())));
        }
    }
    
    private void toIR(LoadInst inst, List<Stmt> ret) {
        Lval lval = Reg.getReg(inst.dest());
        Expr e = toExpr(inst.src());
        ret.add(new LdStmt(inst, lval, e));
    }
    
    private void toIR(LoadPairInst inst, List<Stmt> ret) {
        Reg first = Reg.getReg(inst.destLeft());
        int sizeInBytes = first.sizeInBits() / 8;
        Expr addr = toExpr(inst.src());
        ret.add(new LdStmt(inst, first, addr));
        Reg second = Reg.getReg(inst.destRight());
        Const disp = Const.getConstant(sizeInBytes);
        ret.add(new LdStmt(inst, second, new BExpr(BExpr.Op.ADD, addr, disp)));
    }
    
    public void toIR(PopInst inst, List<Stmt> ret) {
        int n = inst.numOfPoppedRegisters();
        Reg sp = Reg.getReg(Register.getRegister(Register.Id.SP));
        
        if (n == 1) {
            Reg popped = Reg.getReg(inst.firstPoppedRegister());
            ret.add(new LdStmt(inst, popped, sp));
            Expr add = new BExpr(BExpr.Op.ADD, sp, Const.getConstant(popped.sizeInBits() / 8));
            ret.add(new AssignStmt(inst, sp, add));
        } else {
            for (Register p : inst.poppedRegisters()) {
                Reg popped = Reg.getReg(p);
                ret.add(new LdStmt(inst, popped, sp));
                Expr add = new BExpr(BExpr.Op.ADD, sp,
                        Const.getConstant(popped.sizeInBits() / 8));
                ret.add(new AssignStmt(inst, sp, add));
            }
        }
    }
    
    public void toIR(PushInst inst, List<Stmt> ret) {
        int n = inst.numOfPushedRegisters();
        Reg sp = Reg.getReg(Register.getRegister(Register.Id.SP));
        
        if (n == 1) {
            Reg popped = Reg.getReg(inst.firstPushedRegister());
            ret.add(new LdStmt(inst, popped, sp));
            Expr sub = new BExpr(BExpr.Op.SUB, sp, Const.getConstant(popped.sizeInBits() / 8));
            ret.add(new AssignStmt(inst, sp, sub));
        } else {
            for (Register p : inst.pushedRegisters()) {
                Reg popped = Reg.getReg(p);
                ret.add(new LdStmt(inst, popped, sp));
                Expr sub = new BExpr(BExpr.Op.SUB, sp,
                        Const.getConstant(popped.sizeInBits() / 8));
                ret.add(new AssignStmt(inst, sp, sub));
            }
        }
    }
    
    private void toIR(StoreInst inst, List<Stmt> ret) {
        Expr addr = toExpr(inst.dest());
        Expr e = toExpr(inst.src());
        ret.add(new StStmt(inst, addr, e));
    }
    
    private void toIR(StorePairInst inst, List<Stmt> ret) {
        Reg first = Reg.getReg(inst.srcLeft());
        int sizeInBytes = first.sizeInBits() / 8;
        Expr addr = toExpr(inst.dest());
        ret.add(new LdStmt(inst, first, addr));
        Reg second = Reg.getReg(inst.srcRight());
        Const disp = Const.getConstant(sizeInBytes);
        ret.add(new LdStmt(inst, second, new BExpr(BExpr.Op.ADD, addr, disp)));
    }
    
    private void toIR(UnaryArithInst inst, List<Stmt> ret) {
        Lval lval = Reg.getReg(inst.dest());
        switch (inst.opcode().mnemonic()) {
            case ADR:
                ret.add(new AssignStmt(inst, lval, operandToExpr(inst.src())));
                break;
            case NEG:
                ret.add(new AssignStmt(inst, lval,
                        new BExpr(BExpr.Op.SUB, Const.getConstant(0),
                                operandToExpr(inst.src()))));
                break;
            default:
                Assert.unreachable();
        }
    }
    
    private Expr toExpr(Condition cond) {
        Expr ret = null;
        switch (cond) {
            case AL:
                ret = Const.getConstant(1);
                break;
            case EQ:
                // Z == 1
                ret = Flg.getFlg(Flag.Z);
                break;
            case GE:
                // N == V
                ret = new UExpr(UExpr.Op.NOT,
                        new BExpr(BExpr.Op.SUB, Flg.getFlg(Flag.N), Flg.getFlg(Flag.V)));
                break;
            case GT:
                // Z == 0 && N == V
                ret = new BExpr(BExpr.Op.AND,
                        new UExpr(UExpr.Op.NOT, Flg.getFlg(Flag.Z)),
                        new UExpr(UExpr.Op.NOT, new BExpr(BExpr.Op.SUB, Flg.getFlg(Flag.N), Flg.getFlg(Flag.V))));
                break;
            case HI:
                // C ==1 && Z == 0
                ret = new BExpr(BExpr.Op.AND,
                        Flg.getFlg(Flag.C),
                        new UExpr(UExpr.Op.NOT, Flg.getFlg(Flag.Z)));
                break;
            case HS:
                // C == 1
                ret = Flg.getFlg(Flag.C);
                break;
            case LE:
                // Z == 1 || N != V
                ret = new BExpr(BExpr.Op.OR,
                        Flg.getFlg(Flag.Z),
                        new BExpr(BExpr.Op.SUB, Flg.getFlg(Flag.N), Flg.getFlg(Flag.Z)));
                break;
            case LO:
                // C == 0
                ret = new UExpr(UExpr.Op.NOT, Flg.getFlg(Flag.C));
                break;
            case LS:
                // C == 0 || Z == 1
                ret = new BExpr(BExpr.Op.OR,
                        Flg.getFlg(Flag.Z),
                        new UExpr(UExpr.Op.NOT, Flg.getFlg(Flag.C)));
                break;
            case LT:
                // N != V
                ret = new BExpr(BExpr.Op.SUB,
                        Flg.getFlg(Flag.N),
                        Flg.getFlg(Flag.V));
                break;
            case MI:
                // N == 1
                ret = Flg.getFlg(Flag.N);
                break;
            case NE:
                // Z == 0
                ret = new UExpr(UExpr.Op.NOT, Flg.getFlg(Flag.Z));
                break;
            case PL:
                // N == 0
                ret = new UExpr(UExpr.Op.NOT, Flg.getFlg(Flag.N));
                break;
            case VC:
                // V == 0
                ret = new UExpr(UExpr.Op.NOT, Flg.getFlg(Flag.V));
                break;
            case VS:
                // V == 1
                ret = Flg.getFlg(Flag.V);
                break;
            default:
                break;
            
        }
        return ret;
    }
    
    private void toIR(SelectInst inst, List<Stmt> ret) {
        Expr cond = toExpr(inst.condition());
        ret.add(new SelStmt(inst, Reg.getReg(inst.dest()),
                cond, toExpr(inst.truevalue()), toExpr(inst.falsevalue())));
    }
    
    public List<Stmt> toIRStatements(Instruction inst) {
        LinkedList<Stmt> ret = new LinkedList<Stmt>();
        switch (inst.kind()) {
            case BIN_ARITHN:
                toIR((BinaryArithInst)inst, ret);
                break;
            case BITFIELD_MOVE:
                toIR((BitfieldMoveInst)inst, ret);
                break;
            case BRANCH:
                toIR((BranchInst)inst, ret);
                break;
            case COMPARE:
                toIR((CompareInst)inst, ret);
                break;
            case LOAD:
                toIR((LoadInst)inst, ret);
                break;
            case LOAD_PAIR:
                toIR((LoadPairInst)inst, ret);
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
            case STORE:
                toIR((StoreInst)inst, ret);
                break;
            case STORE_PAIR:
                toIR((StorePairInst)inst, ret);
                break;
            case UN_ARITH:
                toIR((UnaryArithInst)inst, ret);
                break;
            case NOP:
                break;
        }
        return ret;
    }

}

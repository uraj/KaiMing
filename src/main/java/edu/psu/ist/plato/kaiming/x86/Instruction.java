package edu.psu.ist.plato.kaiming.x86;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Label;
import edu.psu.ist.plato.kaiming.util.ArrayIterator;
import edu.psu.ist.plato.kaiming.util.Assert;

public abstract class Instruction extends Entry implements Iterable<Operand> {

    protected long mAddr;
    protected final Opcode mOpcode;
    private Operand[] mOperands;
    
    private final static Set<Flag> sModifiedFlags;
    
    static {
        sModifiedFlags = Collections.unmodifiableSet(new HashSet<Flag>());
    }

    protected Instruction(long addr, Opcode op, Operand[] operands) {
        mAddr = addr;
        mOpcode = op;
        mOperands = operands;
    }
    
    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printInstruction(this);
        p.close();
        return baos.toString();
    }

    public final long getAddr() {
        return mAddr;
    }

    public final void setAddr(long addr) {
        mAddr = addr;
    }

    public final Opcode getOpcode() {
        return mOpcode;
    }

    public final void setOperand(int index, Operand operand) {
        mOperands[index] = operand;
    }

    public final Operand getOperand(int index) {
        return mOperands[index];
    }
    
    public final Operand[] getOperands() {
        return Arrays.copyOf(mOperands, mOperands.length);
    }

    public final int getNumOperands() {
        return mOperands.length;
    }

    public final boolean isJumpInst() {
        return this instanceof JumpInst;
    }
    
    public final boolean isUncondJumpInst() {
        return this instanceof JumpInst && !isConditional();
    }

    public final boolean isCondJumpInst() {
        return this instanceof CondJumpInst;
    }

    public final boolean isCondSetInst() {
        return this instanceof CondSetInst;
    }
    
    public final boolean isCondMoveInst() {
        return this instanceof CondMoveInst;
    }
    
    public final boolean isMoveInst() {
        return this instanceof MoveInst;
    }
    
    public final boolean isLeaInst() {
        return this instanceof LeaInst;
    }
    
    public final boolean isUncondMoveInst() {
        return this instanceof MoveInst && !isConditional();
    }

    public final boolean isBinaryArithInst() {
        return this instanceof BinaryArithInst;
    }
    
    public final boolean isMultiplyInst() {
        return this instanceof MultiplyInst;
    }
    
    public final boolean isDivideInst() {
        return this instanceof DivideInst;
    }
    
    public final boolean isBitTestInst() {
        return this instanceof BitTestInst;
    }
    
    public final boolean isExchangeInst() {
        return this instanceof ExchangeInst;
    }

    public final boolean isCompareInst() {
        return this instanceof CompareInst;
    }

    public final boolean isUnaryArithInst() {
        return this instanceof UnaryArithInst;
    }

    public final boolean isPopInst() {
        return this instanceof PopInst;
    }

    public final boolean isPushInst() {
        return this instanceof PushInst;
    }

    public final boolean isCallInst() {
        return this instanceof CallInst;
    }

    public final boolean isReturnInst() {
        return this instanceof ReturnInst;
    }

    public final boolean isBranchInst() {
        return this instanceof BranchInst;
    }
    
    public final boolean isRepeatInst() {
        return this instanceof RepeatInst;
    }
    
    public final boolean isOtherInst() {
        return this instanceof OtherInst;
    }
    
    public final boolean isNopInst() {
        return this instanceof NopInst;
    }
    
    public final boolean isTerminator() {
        return isReturnInst() || isJumpInst() || isCondJumpInst();
    }
    
    public boolean isConditional() {
        return false;
    }

    @Override
    public Iterator<Operand> iterator() {
        return new ArrayIterator<Operand>(mOperands);
    }

    public static Instruction createInstruction(long addr, Opcode opcode,
            Operand[] operands, boolean isIndirect) {
        Instruction ret = null;
        switch (opcode.getOpcodeClass()) {
            case ADD:
            case ADC:
            case SUB:
            case SBB:
            case AND:
            case XOR:
            case OR:
                Assert.test(operands.length == 2);
                ret = new BinaryArithInst(addr, opcode, operands[0], operands[1]);
                break;
            case SAR:
            case SHL:
            case SHR:
                Assert.test(operands.length == 2 || operands.length == 1);
                Operand op1 = operands[0];
                Operand op2;
                if (operands.length == 1)
                    op2 = Immediate.getImmediate(1);
                else
                    op2 = operands[1];
                ret = new BinaryArithInst(addr, opcode, op1, op2);
                break;
            case MUL:
                ret = new MultiplyInst(addr, opcode, operands);
                break;
            case DIV:
                Assert.test(operands.length == 1);
                ret = new DivideInst(addr, opcode, operands[0]);
                break;
            case BT:
                Assert.test(operands.length == 2);
                ret = new BitTestInst(addr, opcode, operands[0], operands[1]);
                break;
            case XCHG:
                Assert.test(operands.length == 2);
                ret = new ExchangeInst(addr, opcode, operands[0], operands[1]);
                break;
            case TEST:
            case CMP:
                Assert.test(operands.length == 2);
                ret = new CompareInst(addr, opcode, operands[0], operands[1]);
                break;
            case INC:
            case DEC:
            case NEG:
            case NOT:
            case BSWAP:
                Assert.test(operands.length == 1);
                ret = new UnaryArithInst(addr, opcode, operands[0]);
                break;
            case SETCC:
                Assert.test(operands.length == 1);
                ret = new CondSetInst(addr, opcode, operands[0]);
                break;
            case JMP:
                Assert.test(operands.length == 1);
                if (operands[0].isMemory())
                    ret = new JumpInst(addr, opcode, operands[0].asMemory(), isIndirect);
                else if (operands[0].isRegister())
                    ret = new JumpInst(addr, opcode, operands[0].asRegister(), isIndirect);
                else
                    Assert.test(false);
                break;
            case JCC:
                Assert.test(operands.length == 1);
                if (operands[0].isMemory())
                    ret = new CondJumpInst(addr, opcode, operands[0].asMemory(), isIndirect);
                else if (operands[0].isRegister())
                    ret = new CondJumpInst(addr, opcode, operands[0].asRegister(), isIndirect);
                else
                    Assert.test(false);
                break;
            case NOP:
                Assert.test(operands.length == 0);
                ret = new NopInst(addr);
                break;
            case CALL:
                Assert.test(operands.length == 1);
                if (operands[0].isMemory())
                    ret = new CallInst(addr, opcode, operands[0].asMemory(), isIndirect);
                else if (operands[0].isRegister())
                    ret = new CallInst(addr, opcode, operands[0].asRegister(), isIndirect);
                else
                    Assert.test(false);
                break;
            case RET:
                Assert.test(operands.length <= 1);
                ret = new ReturnInst(addr, opcode);
                break;
            case LEA:
                Assert.test(operands.length == 2);
                Assert.test(operands[0].isMemory());
                Assert.test(operands[1].isRegister());
                ret = new LeaInst(addr, opcode, operands[0].asMemory(), operands[1].asRegister());
                break;
            case MOV:
                Assert.test(operands.length == 2);
                ret = new MoveInst(addr, opcode, operands[0], operands[1]);
                break;
            case CMOV:
                Assert.test(operands.length == 2);
                ret = new CondMoveInst(addr, opcode, operands[0], operands[1]);
                break;
            case PUSH:
                Assert.test(operands.length == 1);
                ret = new PushInst(addr, opcode, operands[0]);
                break;
            case POP:
                Assert.test(operands.length == 1);
                Assert.test(operands[0] instanceof Register);
                ret = new PopInst(addr, opcode, (Register) operands[0]);
                break;
            case REP:
                ret = new RepeatInst(addr, opcode, operands);
                break;
            default:
                ret = new OtherInst(addr, opcode, operands);
                break;
        }
        return ret;
    }

    @Override
    public int fillLabelInformation(Label l) {
        Assert.test(l instanceof AsmLabel);
        AsmLabel nl = (AsmLabel) l;
        nl.setAddr(mAddr);
        return 0;
    }

    @Override
    public int fillLabelInformation(Label l, Entry e) {
        return 0;
    }

    @Override
    public long getIndex() {
        return mAddr;
    }
    
    public Set<Flag> getModifiedFlags() {
        return sModifiedFlags;
    }
    
    public final boolean modifyFlags() {
        return getModifiedFlags().size() != 0;
    }
}

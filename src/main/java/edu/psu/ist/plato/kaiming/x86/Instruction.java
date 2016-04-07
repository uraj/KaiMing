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

	public enum Kind {
		BIN_ARITH,
		UN_ARITH,
		CALL,
		RETURN,
		COMPARE,
		DIVIDE,
		MULTIPLY,
		BIT_TEST,
		COND_SET,
		EXCHANGE,
		JUMP,
		LEA,
		MOVE,
		MOVE_STR,
		NOP,
		POP,
		PUSH,
		OTHER,
	};
	
	private final Kind mKind;
    protected long mAddr;
    protected final Opcode mOpcode;
    private Operand[] mOperands;
    
    private final static Set<Flag> sModifiedFlags;
    
    public final Kind kind() {
    	return mKind;
    }
    
    static {
        sModifiedFlags = Collections.unmodifiableSet(new HashSet<Flag>());
    }

    protected Instruction(Kind kind, long addr, Opcode op, Operand[] operands) {
    	mKind = kind;
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

    public final long addr() {
        return mAddr;
    }

    public final void setAddr(long addr) {
        mAddr = addr;
    }

    public final Opcode opcode() {
        return mOpcode;
    }

    protected final void setOperand(int index, Operand operand) {
        mOperands[index] = operand;
    }

    public final Operand operand(int index) {
        return mOperands[index];
    }
    
    public final Operand[] operands() {
        return Arrays.copyOf(mOperands, mOperands.length);
    }

    public final int numOfOperands() {
        return mOperands.length;
    }

    public final boolean isJumpInst() {
        return mKind.equals(Kind.JUMP);
    }
    
    public final boolean isUncondJumpInst() {
        return mKind.equals(Kind.JUMP) && !isConditional();
    }

    public final boolean isCondJumpInst() {
        return mKind.equals(Kind.JUMP) && isConditional();
    }

    public final boolean isCondSetInst() {
        return mKind.equals(Kind.COND_SET);
    }
    
    public final boolean isMoveInst() {
    	return mKind.equals(Kind.MOVE);
    }
    
    public final boolean isCondMoveInst() {
        return isMoveInst() && isConditional();
    }
    
    public final boolean isLeaInst() {
        return mKind.equals(Kind.LEA);
    }
    
    public final boolean isUncondMoveInst() {
        return isMoveInst() && !isConditional();
    }

    public final boolean isBinaryArithInst() {
        return mKind.equals(Kind.BIN_ARITH);
    }
    
    public final boolean isMultiplyInst() {
        return mKind.equals(Kind.MULTIPLY);
    }
    
    public final boolean isDivideInst() {
        return mKind.equals(Kind.DIVIDE);
    }
    
    public final boolean isBitTestInst() {
        return mKind.equals(Kind.BIT_TEST);
    }
    
    public final boolean isExchangeInst() {
        return mKind.equals(Kind.EXCHANGE);
    }

    public final boolean isCompareInst() {
        return mKind.equals(Kind.COMPARE);
    }

    public final boolean isUnaryArithInst() {
        return mKind.equals(Kind.UN_ARITH);
    }

    public final boolean isPopInst() {
        return mKind.equals(Kind.POP);
    }

    public final boolean isPushInst() {
        return mKind.equals(Kind.PUSH);
    }

    public final boolean isCallInst() {
        return mKind.equals(Kind.CALL);
    }

    public final boolean isReturnInst() {
        return mKind.equals(Kind.RETURN);
    }

    public final boolean isBranchInst() {
        return this instanceof BranchInst;
    }
    
    public final boolean isOtherInst() {
        return mKind.equals(Kind.OTHER);
    }
    
    public final boolean isNopInst() {
        return mKind.equals(Kind.NOP);
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
        switch (opcode.opcodeClass()) {
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
                Assert.test(!(operands[0].isMemory() && operands[1].isMemory()));
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
            case MOVS:
                ret = new MoveStrInst(addr, opcode,
                        new Memory(null, 0, Register.getRegister(Register.Id.ESI), null, 1),
                        new Memory(null, 0, Register.getRegister(Register.Id.EDI), null, 1));
                break;
            default:
                ret = new OtherInst(addr, opcode, operands);
                break;
        }
        return ret;
    }
    
    public int fillLabelInformation(Label l) {
        l.setAddr(mAddr);
        return 0;
    }

    @Override
    public long index() {
        return mAddr;
    }
    
    public Set<Flag> modifiedFlags() {
        return sModifiedFlags;
    }
    
    public final boolean modifyAnyFlag() {
        return modifiedFlags().size() != 0;
    }
    
    public final boolean isRepeated() {
        return mOpcode.isRepeated();
    }
    
    public final boolean isLocked() {
        return mOpcode.isLocked();
    }
}

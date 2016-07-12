package edu.psu.ist.plato.kaiming.arm64;

import java.util.Iterator;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Label;
import edu.psu.ist.plato.kaiming.Machine;
import edu.psu.ist.plato.kaiming.arm64.LoadStoreInst.AddressingMode;
import edu.psu.ist.plato.kaiming.util.ArrayIterator;
import edu.psu.ist.plato.kaiming.util.Assert;

public class Instruction extends Entry implements Iterable<Operand> {
    
    public enum Kind {
        BIN_ARITHN,
        UN_ARITH,
        LOAD,
        LOAD_PAIR,
        STORE,
        STORE_PAIR,
        BRANCH,
        NOP,
        MOVE,
        COMPARE,
        SELECT,
        BITFIELD_MOVE,
    }
    
    private Kind mKind;
    private long mAddr;
    private Opcode mOpcode;
    private Operand[] mOperands;
    
    protected Instruction(Kind kind, long addr, Opcode op, Operand[] operands) {
        mKind = kind;
        mAddr = addr;
        mOpcode = op;
        mOperands = operands;
    }
    
    public long addr() {
        return mAddr;
    }
    
    public Kind kind() {
        return mKind;
    }
    
    public Opcode opcode() {
        return mOpcode;
    }
    
    protected final Operand operand(int index) {
        return mOperands[index];
    }
    
    protected final int numOfOperands() {
        return mOperands.length;
    }
    
    protected final void setOperand(int index, Operand op) {
        mOperands[index] = op;
    }
    
    // This method should not be used inside Instruction and its subclasses.
    // By override this method, specific instructions can ``hide'' certain 
    // operands from outside. This is useful for implementing alias instructions
    // which are frequently seen in ARM64
    @Override
    public Iterator<Operand> iterator() {
        return new ArrayIterator<Operand>(mOperands);
    }

    @Override
    public long index() {
        return mAddr;
    }
    
    public int fillLabelInformation(Label l) {
        l.setAddr(mAddr);
        return 0;
    }
    
    @Override
    public final Machine machine() {
        return ARM64Machine.instance;
    }
    
    public static Instruction create(long addr, Opcode opcode, Operand[] oplist,
            Condition cond, boolean preidx) {
        Instruction ret = null;
        cond = cond == null ? Condition.AL : cond; 
        switch (opcode.mnemonic()) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case ASR:
            case LSL:
            case LSR:
            case ORR:
            case ORN:
            case AND:
                Assert.verify(oplist.length == 3 ||  oplist.length == 2);
                Assert.verify(oplist[0].isRegister());
                if (oplist.length == 3)
                    ret = new BinaryArithInst(addr, opcode, oplist[0].asRegister(), oplist[1], oplist[2]);
                else if (oplist.length == 2)
                    ret = new BinaryArithInst(addr, opcode, oplist[0].asRegister(),
                            oplist[0].asRegister(), oplist[1]);
                break;
            case ADR:
            case NEG:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister() && (oplist[1].isRegister() || oplist[1].isImmeidate()));
                ret = new UnaryArithInst(addr, opcode, oplist[0].asRegister(), oplist[1]);
                break;
            case LDR:
            case STR: {
                Assert.verify(oplist.length == 2 || (oplist.length == 3));
                Assert.verify(oplist[0].isRegister() && oplist[1].isMemory());
                
                Register rd = oplist[0].asRegister();
                Memory mem = oplist[1].asMemory();
                AddressingMode mode = preidx ? AddressingMode.PRE_INDEX : AddressingMode.REGULAR;
                if (oplist.length == 3) {
                    Assert.verify(!preidx && oplist[2].isImmeidate());
                    mem = new Memory(mem.base(), oplist[2].asImmediate().value());
                    mode = AddressingMode.POST_INDEX;
                }
                if (opcode.mnemonic() == Opcode.Mnemonic.LDR)
                    ret = new LoadInst(addr, opcode, rd, mem, mode);
                else
                    ret = new StoreInst(addr, opcode, rd, mem, mode);
                break;
            }
            case LDP:
            case STP: {
                Assert.verify(oplist.length == 3 || oplist.length == 4);
                Assert.verify(oplist[0].isRegister() && oplist[1].isRegister() && oplist[2].isMemory());
                
                Register rd1 = oplist[0].asRegister();
                AddressingMode mode = preidx ? AddressingMode.PRE_INDEX : AddressingMode.REGULAR;
                Register rd2 = oplist[1].asRegister();
                Memory mem = oplist[2].asMemory();
                if (oplist.length == 4) {
                    Assert.verify(!preidx && oplist[3].isImmeidate());
                    mode = AddressingMode.POST_INDEX;
                    mem = new Memory(mem.base(), oplist[3].asImmediate().value());
                }
                if (opcode.mnemonic() == Opcode.Mnemonic.LDP)
                    ret = new LoadPairInst(addr, opcode, rd1, rd2, mem, mode);
                else
                    ret = new StorePairInst(addr, opcode, rd1, rd2, mem, mode);
                break;
            }
            case TST:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister() && (oplist[1].isRegister() || oplist[1].isImmeidate()));
                ret = new CompareInst(addr, opcode, oplist[0].asRegister(), oplist[1], true);
                break;
            case CMP:
            case CMN:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister() && (oplist[1].isRegister() || oplist[1].isImmeidate()));
                ret = new CompareInst(addr, opcode, oplist[0].asRegister(), oplist[1], false);
                break;
            case CSEL:
            case CSINC:
                Assert.verify(oplist.length == 3);
                Assert.verify(oplist[0].isRegister() && 
                        oplist[1].isRegister() && oplist[2].isRegister());
                ret = new SelectInst(addr, opcode, oplist[0].asRegister(),
                        oplist[1].asRegister(), oplist[2].asRegister(), cond);
                break;
            case CINC:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister() && oplist[1].isRegister());
                ret = new SelectInst(addr, new Opcode("CSINC"), oplist[0].asRegister(),
                        oplist[1].asRegister(), oplist[1].asRegister(), cond.invert());
                break;
            case CSET: {
                Assert.verify(oplist.length == 1);
                Assert.verify(oplist[0].isRegister());
                Register zero = oplist[0].asRegister().sizeInBits() == 32 ?
                        Register.get(Register.Id.WZR) : Register.get(Register.Id.XZR); 
                ret = new SelectInst(addr, new Opcode("CSINC"), oplist[0].asRegister(),
                        zero, zero, cond.invert());
                break;
            }
            case MOV:
            case MOVK:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister());
                ret = new MoveInst(addr, opcode, oplist[0].asRegister(), oplist[1]);
                break;
            case EXT:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister() && oplist[1].isRegister());
                ret = new ExtensionInst(addr, opcode,
                        oplist[0].asRegister(), oplist[1].asRegister());
                break;
            case BFM:
                Assert.verify(oplist.length == 4);
                ret = new BitfieldMoveInst(addr, opcode, oplist[0].asRegister(),
                        oplist[1].asRegister(), oplist[2].asImmediate(), oplist[3].asImmediate());
                break;
            case B:
            case BL:
                Assert.verify(oplist.length <= 1);
                if (oplist.length == 1) {
                    Assert.verify(oplist[0].isMemory() || oplist[0].isRegister());
                    ret = new BranchInst(addr, opcode, oplist[0]);
                } else {
                    ret = new ReturnInst(addr, opcode);
                }
                break;
            case NOP:
                Assert.verify(oplist.length == 0);
                ret = new NopInst(addr);
                break;
        }
        Assert.verify(ret != null);
        return ret;
    }
}

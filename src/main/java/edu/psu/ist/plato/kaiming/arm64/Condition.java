package edu.psu.ist.plato.kaiming.arm64;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum Condition {
    AL(new Flag[] {}), NV(new Flag[] {}),
    EQ(new Flag[] {Flag.Z}),
    NE(new Flag[] {Flag.Z}),
    HS(new Flag[] {Flag.C}),
    LO(new Flag[] {Flag.C}),
    MI(new Flag[] {Flag.N}),
    PL(new Flag[] {Flag.N}),
    VS(new Flag[] {Flag.V}),
    VC(new Flag[] {Flag.V}),
    HI(new Flag[] {Flag.C, Flag.Z}),
    LS(new Flag[] {Flag.C, Flag.Z}),
    GE(new Flag[] {Flag.N, Flag.V}),
    LT(new Flag[] {Flag.N, Flag.V}),
    GT(new Flag[] {Flag.N, Flag.V, Flag.Z}),
    LE(new Flag[] {Flag.N, Flag.V, Flag.Z});
    
    private Set<Flag> mDependentFlags;
    
    private Condition(Flag[] dependentFlags) {
        mDependentFlags = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(dependentFlags)));
    }
    
    public Set<Flag> dependentFlags() {
        return mDependentFlags;
    }
    
    public Condition invert() {
        Condition ret = null;
        switch (this) {
            case EQ:
                ret = NE;
            case NE:
                ret = EQ;
                break;
            case GE:
                ret = LT;
                break;
            case LT:
                ret = GE;
                break;
            case GT:
                ret = LE;
                break;
            case LE:
                ret = GT;
                break;
            case HI:
                ret = LS;
                break;
            case LS:
                ret = HI;
                break;
            case HS:
                ret = LO;
                break;
            case LO:
                ret = HS;
                break;
            case MI:
                ret = PL;
                break;
            case PL:
                ret = MI;
                break;
            case VC:
                ret = VS;
                break;
            case VS:
                ret = VC;
                break;
            default: 
                // AL and NV not handled. I don't want to support AL or NV
                // at this point, because trying to invert them usually
                // suggests irregular (or even illegal) assembly code and
                // therefore a bug of other parts of the project, because
                // it is unlikely that we get irregular assembly code from
                // disassembled binary
                ;
        }
        if (ret == null)
            throw new UnsupportedOperationException();
        return ret;
    }
}

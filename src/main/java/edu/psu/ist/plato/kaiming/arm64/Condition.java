package edu.psu.ist.plato.kaiming.arm64;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum Condition {
    AL(new Flag[] {}),
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
}

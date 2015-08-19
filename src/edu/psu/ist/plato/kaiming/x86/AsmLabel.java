package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.Label;

public class AsmLabel extends Label {
    private long mAddr;
    private String mName;

    public AsmLabel(String name, long addr) {
        mName = name;
        mAddr = addr;
    }

    @Override
    public String toString() {
        return mName;
    }

    public long getAddr() {
        return mAddr;
    }

    public void setAddr(long addr) {
        mAddr = addr;
    }

    @Override
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }
}

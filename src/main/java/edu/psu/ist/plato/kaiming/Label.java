package edu.psu.ist.plato.kaiming;

public class Label {
	
	private long mAddr;
    private String mName;

    public Label(String name, long addr) {
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

    public String getName() {
        return mName;
    }

    public boolean equals(Label l) {
        return l.mName == mName && l.mAddr == mAddr;
    }
    
    public void setAddr(long addr) {
        mAddr = addr;
    }
    
}

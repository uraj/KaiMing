package edu.psu.ist.plato.kaiming.arm64;

public class Opcode {
    private final String mCode;
    public Opcode(String mnemonic) {
        mCode = mnemonic;
    }
    
    public String rawOpcode() {
        return mCode;
    }
}

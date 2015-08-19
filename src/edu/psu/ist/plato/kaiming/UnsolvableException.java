package edu.psu.ist.plato.kaiming;

public class UnsolvableException extends Exception {

    private static final long serialVersionUID = -3063483450217610783L;
    
    public enum Reason {
        SOLVED,
        REACH_MAX_ITERATION,
        UNSUPPORTED_INSTANCE
    }
    
    public final Reason reason;
    
    public UnsolvableException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

}

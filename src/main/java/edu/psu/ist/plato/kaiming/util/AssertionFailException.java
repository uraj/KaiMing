package edu.psu.ist.plato.kaiming.util;

public class AssertionFailException extends RuntimeException {

    public enum Cause {
        UNSUPPORTED_LANGUAGE,
        CONDITION_UNFEASIBLE,
    }
    
    public final Cause cause;
    
    public AssertionFailException(String message, Cause cause) {
        super(message);
        this.cause = cause;
    }

    public AssertionFailException(String message) {
        super(message);
        this.cause = Cause.CONDITION_UNFEASIBLE;
    }
    
    private static final long serialVersionUID = -805002875505904676L;

}

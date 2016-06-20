package edu.psu.ist.plato.kaiming.exception;

public class AssertionFailException extends RuntimeException {
    
    private static final long serialVersionUID = -416065883182958214L;
    
    public AssertionFailException(String message) {
        super(message);
    }
    
}

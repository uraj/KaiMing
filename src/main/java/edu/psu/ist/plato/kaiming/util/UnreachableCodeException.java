package edu.psu.ist.plato.kaiming.util;

public class UnreachableCodeException extends RuntimeException {

    private static final long serialVersionUID = -416065883182958214L;

    public UnreachableCodeException() {
        super();
    }
    
    public UnreachableCodeException(String message) {
        super(message);
    }
}

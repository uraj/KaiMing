package edu.psu.ist.plato.kaiming.util;

import edu.psu.ist.plato.kaiming.exception.AssertionFailException;
import edu.psu.ist.plato.kaiming.exception.UnreachableCodeException;

public class Assert {

    public static void test(boolean condition) {
        if (!condition)
            throw new AssertionFailException("Assertion failed.");
    }
    
    public static void test(boolean condition, Object info) {
        if (!condition)
            throw new AssertionFailException(info.toString());
    }
    
    public static void test(boolean condition, long info) {
        if (!condition)
            throw new AssertionFailException(Long.toHexString(info));
    }
    
    public static void unreachable() {
        throw new UnreachableCodeException();
    }
    
    public static void unreachable(String msg) {
        throw new UnreachableCodeException(msg);
    }
    
    public static void verify(boolean condition) {
        verify(condition, "");
    }
    
    public static void verify(boolean condition, String msg) {
        if (!condition)
            throw new IllegalArgumentException(msg);
    }
    
    /**
     * A debug-use wrapper of @Assert.test 
     */
    public static void debug(boolean condition) {
        test(condition);
    }
    
    public static void debug(boolean condition, String msg) {
        test(condition, msg);
    }
}

package edu.psu.ist.plato.kaiming.util;

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
}

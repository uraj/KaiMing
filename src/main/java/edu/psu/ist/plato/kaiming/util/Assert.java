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
    
    public static void unreachable() {
        test(false, "Uncreachable code");
    }
    
    public static void unreachable(String msg) {
        test(false, "Uncreachable code: " + msg);
    }
    
    public static void verify(boolean condition) {
        if (!condition)
            throw new IllegalArgumentException();
    }
}

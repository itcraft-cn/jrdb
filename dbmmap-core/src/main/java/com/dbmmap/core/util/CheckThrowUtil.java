package com.dbmmap.core.util;

public final class CheckThrowUtil {
    
    public static void ifMatchThrow(boolean condition, String message, Object... args) {
        if (condition) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }
    
    private CheckThrowUtil() {}
}
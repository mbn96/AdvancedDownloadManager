package com.mbn;

import java.util.Arrays;

public class ConsoleUtils {

    private static volatile int PREVIOUS_LINE_SIZE;
    private static final Object LOCK = new Object();

    public static void println(String str) {
        synchronized (LOCK) {
            System.out.println(str);
            PREVIOUS_LINE_SIZE = str.length() + 1;
        }
    }

    public static void print(String str) {
        synchronized (LOCK) {
            System.out.print(str);
            PREVIOUS_LINE_SIZE = str.length();
        }
    }

    public static void clearLastLine() {
        synchronized (LOCK) {
            clear(PREVIOUS_LINE_SIZE);
            PREVIOUS_LINE_SIZE = 0;
        }
    }

    public static void clear(int chars) {
        synchronized (LOCK) {
            char[] backspaces = new char[chars];
            Arrays.fill(backspaces, '\b');
            System.out.print(new String(backspaces));
        }
    }
}

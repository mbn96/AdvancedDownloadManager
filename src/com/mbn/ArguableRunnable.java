package com.mbn;

public abstract class ArguableRunnable<A> implements Runnable {
    private final A arg;

    public ArguableRunnable(A arg) {
        this.arg = arg;
    }

    public A getArg() {
        return arg;
    }

    @Override
    public void run() {
        run(arg);
    }

    public abstract void run(A args);

}

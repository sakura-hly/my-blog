package com.moon.demo;

public class Semaphore {
    private boolean signal = false;

    public synchronized void take() {
        this.signal = true;
        notify();
    }

    public synchronized void release() throws InterruptedException {
        while (!signal) {
            wait();
        }
        signal = false;
    }
}

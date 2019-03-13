package com.moon.demo;

public class BoundedSemaphore {
    private int signals = 0;
    private int bound = 0;

    public BoundedSemaphore(int upperBound) {
        this.bound = upperBound;
    }

    public synchronized void take() throws InterruptedException {
        while (signals == bound) {
            wait();
        }
        signals++;
        notify();
    }

    public synchronized void release() throws InterruptedException {
        while (signals == 0) {
            wait();
        }
        signals--;
        notify();
    }
}

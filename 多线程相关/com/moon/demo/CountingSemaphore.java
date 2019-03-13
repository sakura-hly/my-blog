package com.moon.demo;

public class CountingSemaphore {
    private int signals = 0;

    public synchronized void take() {
        signals++;
        notify();
    }

    public synchronized void release() throws InterruptedException {
        while (signals == 0) wait();
        signals--;
    }
}

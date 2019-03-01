package com.moon.demo;

public class Lock {
    private boolean isLocked = false;

    public synchronized void lock() throws InterruptedException {
        // 自旋锁， 防止虚假唤醒
        while (isLocked) {
            wait();
        }
        isLocked = true;
    }

    public synchronized void unlock() {
        isLocked = false;
        notify();
    }

    public static void main(String[] args) {

    }
}

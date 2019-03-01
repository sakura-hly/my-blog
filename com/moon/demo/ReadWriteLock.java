package com.moon.demo;

public class ReadWriteLock {
    private int readers = 0;
    private int writes = 0;
    private int writeRequests = 0;

    public synchronized void lockRead() throws InterruptedException{
        // 没有线程拥有写锁（writers==0），且没有线程在请求写锁
        while (writes > 0 || writeRequests > 0) {
            wait();
        }
        readers++;
    }

    public synchronized void unlockRaed() {
        readers--;
        notifyAll();
    }

    public synchronized void lockWrite() throws InterruptedException {
        writeRequests++;
        while (readers > 0 || writes > 0) {
            wait();
        }
        writeRequests--;
        writes++;
    }

    public synchronized void unlockWrite() {
        writes--;
        notifyAll();
    }
}

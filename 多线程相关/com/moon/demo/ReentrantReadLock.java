package com.moon.demo;

import java.util.HashMap;
import java.util.Map;

public class ReentrantReadLock {
    private Map<Thread, Integer> readingThreads = new HashMap<>();

    private int writers = 0;
    private int writeRequests = 0;

    public synchronized void lockRead() throws InterruptedException {
        Thread callingThread = Thread.currentThread();
        while (!canGrantReadAccess(callingThread)) {
            wait();
        }
        readingThreads.put(callingThread, getReadAccessCount(callingThread) + 1);
    }

    public synchronized void unlockRead() {
        Thread callingThread = Thread.currentThread();
        int accessCount = getReadAccessCount(callingThread);
        if (accessCount == 1) {
            readingThreads.remove(callingThread);
        } else {
            readingThreads.put(callingThread, accessCount - 1);
        }
        notifyAll();
    }

    private boolean canGrantReadAccess(Thread callingThread) {
        // 在没有线程拥有写锁的情况下才允许读锁的重入。此外，重入的读锁比写锁优先级高
        if (writers > 0) return false;
        if (isReader(callingThread)) return true;
        if (writeRequests > 0) return false;
        return true;
    }

    private int getReadAccessCount(Thread callingThread) {
        return readingThreads.getOrDefault(callingThread, 0);
    }

    private boolean isReader(Thread callingThread) {
        return readingThreads.get(callingThread) != null;
    }
}

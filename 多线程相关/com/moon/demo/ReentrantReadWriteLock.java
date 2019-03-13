package com.moon.demo;

import java.util.HashMap;
import java.util.Map;

public class ReentrantReadWriteLock {
    private Map<Thread, Integer> readingThreads = new HashMap<>();

    private int writeAccesses = 0;
    private int writeRequests = 0;
    private Thread writingThread = null;

    public synchronized void lockRead() throws InterruptedException {
        Thread callingThread = Thread.currentThread();
        while (!canGrantReadAccess(callingThread)) {
            wait();
        }
        readingThreads.put(callingThread, readingThreads.getOrDefault(callingThread, 0) + 1);
    }

    public synchronized void unlockRead() {
        Thread callingThread = Thread.currentThread();
        if (!isReader(callingThread)) {
            throw new IllegalMonitorStateException(
                    "Calling Thread does not" +
                            " hold a read lock on this ReadWriteLock");
        }
        int accessCount = getReadAccessCount(callingThread);
        if (accessCount == 1) {
            readingThreads.remove(callingThread);
        } else {
            readingThreads.put(callingThread, accessCount - 1);
        }
        notifyAll();
    }

    private boolean canGrantReadAccess(Thread callingThread) {
        if (isWriter(callingThread)) return true;
        if (hasWriter(callingThread)) return false;
        if (isReader(callingThread)) return true;
        if (hasWriteRequests()) return false;
        return true;
    }

    public synchronized void lockWrite() throws InterruptedException {
        Thread callingThread = Thread.currentThread();
        writeRequests++;
        while (!canGrantWriteAccess(callingThread)) {
            wait();
        }
        writeRequests--;
        writeAccesses++;
        writingThread = callingThread;
    }

    public synchronized void unlockWrite() {
        Thread callingThread = Thread.currentThread();
        if (!isWriter(callingThread)) {
            throw new IllegalMonitorStateException(
                    "Calling Thread does not" +
                            " hold the write lock on this ReadWriteLock");

        }
        writeAccesses--;
        if (writeAccesses == 0) {
            writingThread = null;
        }
        notifyAll();
    }

    private boolean canGrantWriteAccess(Thread callingThread) {
        if (isOnlyReader(callingThread)) return true;
        if (hasReaders()) return false;
        if (!hasWriter(callingThread)) return true;
        if (!isWriter(callingThread)) return false;
        return true;
    }

    private boolean hasReaders() {
        return readingThreads.size() > 0;
    }

    private boolean isOnlyReader(Thread callingThread) {
        return readingThreads.size() == 1 && readingThreads.containsKey(callingThread);
    }

    private int getReadAccessCount(Thread callingThread) {
        return readingThreads.getOrDefault(callingThread, 0);
    }

    private boolean hasWriteRequests() {
        return writeRequests > 0;
    }

    private boolean isReader(Thread callingThread) {
        return readingThreads.containsKey(callingThread);
    }

    private boolean hasWriter(Thread callingThread) {
        return writingThread != null;
    }

    private boolean isWriter(Thread callingThread) {
        return writingThread == callingThread;
    }
}

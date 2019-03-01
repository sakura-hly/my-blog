package com.moon.demo;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerDemo {
    private static volatile AtomicInteger n = new AtomicInteger(0);

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int i1 = 0; i1 < 10; i1++) {
                    n.incrementAndGet();
                }
            }).start();
        }

        while (Thread.activeCount() > 2) {
            //System.out.println(Thread.activeCount());
            /**
             * IDEA:
             * java.lang.ThreadGroup[name=main,maxpri=10]
             * Thread[main,5,main]
             * Thread[Monitor Ctrl-Break,5,main]
             */
            Thread.yield();
        }
        Thread.currentThread().getThreadGroup().list();
        System.out.println(n.get());
    }
}

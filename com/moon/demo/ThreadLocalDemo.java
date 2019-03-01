package com.moon.demo;

public class ThreadLocalDemo {
    public static class MyRunnable implements Runnable {
        private ThreadLocal<Integer> myThreadLocal = new ThreadLocal<>();

        @Override
        public void run() {
            myThreadLocal.set((int) (Math.random() * 1000));

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(myThreadLocal.get());
        }
    }

    public static void main(String[] args) {
        MyRunnable sharedRunnableInstance = new MyRunnable();

        Thread thread1 = new Thread(sharedRunnableInstance);
        Thread thread2 = new Thread(sharedRunnableInstance);

        thread1.start();
        thread2.start();
    }

}

package com.moon.demo;

public class PendingInterrupt {
    public static void main(String[] args) {
        Thread.currentThread().interrupt();

        //获取当前时间
        long startTime = System.currentTimeMillis();
        try {
            Thread.sleep(2000);
            System.out.println("was NOT interrupted");
        } catch (InterruptedException e) {
            System.out.println("was interrupted");
        }

        //计算中间代码执行的时间
        System.out.println("elapsed time = " + (System.currentTimeMillis() - startTime));
    }
}

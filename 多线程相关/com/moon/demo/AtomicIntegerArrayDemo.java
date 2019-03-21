package com.moon.demo;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class AtomicIntegerArrayDemo {

    public static void main(String[] args) {
        int[] nums = {1, 2, 3, 4, 5, 6};
        AtomicIntegerArray array = new AtomicIntegerArray(nums);
        for (int i = 0; i < nums.length; i++) {
            System.out.println(array.get(i));
        }

        int temp = array.getAndSet(0, 2);
        System.out.println("temp: " + temp + "; array: " + array);

        temp = array.getAndIncrement(0);
        System.out.println("temp: " + temp + "; array: " + array);

        temp = array.getAndAdd(0, 5);
        System.out.println("temp: " + temp + "; array: " + array);
    }
}

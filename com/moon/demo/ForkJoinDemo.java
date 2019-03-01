package com.moon.demo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;

public class ForkJoinDemo {
    class CountTask extends RecursiveTask {
        private static final int THRESHOLD = 2; // 阈值
        private int start;
        private int end;

        public CountTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected Integer compute() {
            int sum = 0;
            boolean canCompute = (end - start) <= THRESHOLD;
            if (canCompute) { //如果任务足够小就计算任务
                for (int i = start; i <= end; i++) {
                    sum += i;
                }
            } else {
                //如果任务大于阀值，就分裂成两个子任务计算Review Date
                int middle = (start + end) >> 1;
                CountTask leftTask = new CountTask(start, middle);
                CountTask rightTask = new CountTask(middle + 1, end);
                //执行子任务
                leftTask.fork();
                rightTask.fork();
                //等待子任务执行完，并得到其结果
                int leftResult = (int) leftTask.join();
                int rightResult = (int) rightTask.join();
                //合并子任务
                sum = leftResult + rightResult;
            }
            return sum;
        }

    }

    public static void main(String[] args) {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        //生成一个计算任务，负责计算1+2+...+100
        CountTask task = new ForkJoinDemo().new CountTask(1, 100);
        Future result = forkJoinPool.submit(task);
        try {
            System.out.println(result.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}

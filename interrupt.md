## 线程中断

### 使用 interrupt()中断线程
当一个线程运行时，另一个线程可以调用对应的Thread对象的 interrupt()方法来中断它，该方法只是在目标线程中设置一个标志，表示它已经被中断，并立即返回。
这里需要注意的是，如果只是单纯的调用 interrupt()方法，线程并没有实际被中断，会继续往下执行。
```
package com.moon.study;

public class SleepInterrupt implements Runnable {
    @Override
    public void run() {
        try {
            System.out.println("in run() - about to sleep for 10 seconds");
            Thread.sleep(10 * 1000);
            System.out.println("in run() - woke up");
        } catch (InterruptedException e) {
            System.out.println("in run() - interrupted while sleeping");
            // 处理完中断异常后，返回到run（）方法
            // 如果没有return，线程不会实际被中断，它会继续打印下面的信息
            return;
        }
        System.out.println("in run() - leaving normally");
    }

    public static void main(String[] args) {
        SleepInterrupt si = new SleepInterrupt();
        Thread t = new Thread(si);
        t.start();

        // 主线程休眠2秒，从而确保刚才启动的线程有机会执行一段时间
        try {
            Thread.sleep(2000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        System.out.println("in main() - interrupting other thread");
        //中断线程t
        t.interrupt();
        System.out.println("in main() - leaving");
    }
}
```
运行结果
```
in run() - about to sleep for 10 seconds
in main() - interrupting other thread
in main() - leaving
in run() - interrupted while sleeping
```
主线程启动新线程后，自身休眠 2 秒钟，允许新线程获得运行时间。新线程打印信息about to sleep for 20 seconds后，继而休眠 20 秒钟，
大约 2 秒钟后，main 线程通知新线程中断，那么新线程的 20 秒的休眠将被打断，从而抛出 InterruptException 异常，执行跳转到 catch 块，
打印出interrupted while sleeping信息，并立即从 run（）方法返回，然后消亡，而不会打印出 catch 块后面的leaving normally信息。

另外，如果将 catch 块中的 return 语句注释掉，则线程在抛出异常后，会继续往下执行，而不会被中断，从而会打印出leaving normally信息。

### 待决中断
在上面的例子中，sleep()方法的实现检查到休眠线程被中断，它会相当友好地终止线程，并抛出 InterruptedException 异常。
另外一种情况，如果线程在调用 sleep()方法前被中断，那么该中断称为待决中断，它会在刚调用 sleep()方法时，立即抛出 InterruptedException 异常。
```
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
```
执行结果如下:
```
was interrupted
elapsed time = 2
```
这种模式下，main 线程中断它自身。除了将中断标志（它是 Thread 的内部标志）设置为 true 外，没有其他任何影响。线程被中断了，但 main 线程仍然运行，main 线程继续监视实时时钟，并进入 try 块，
一旦调用 sleep（）方法，它就会注意到待决中断的存在，并抛出 InterruptException。于是执行跳转到 catch 块，并打印出线程被中断的信息。最后，计算并打印出时间差。

### 使用 isInterrupted()方法判断中断状态
可以在 Thread 对象上调用 isInterrupted()方法来检查任何线程的中断状态。这里需要注意：线程一旦被中断，isInterrupted()方法便会返回 true，
而一旦 sleep()方法抛出异常，它将清空中断标志，此时isInterrupted()方法将返回 false。
```
public class InterruptCheck {
    public static void main(String[] args) {
        Thread t = Thread.currentThread();
        System.out.println("Point A: t.isInterrupted()=" + t.isInterrupted());

        //待决中断，中断自身
        t.interrupt();
        System.out.println("Point B: t.isInterrupted()=" + t.isInterrupted());
        System.out.println("Point C: t.isInterrupted()=" + t.isInterrupted());

        try {
            Thread.sleep(2000);
            System.out.println("was NOT interrupted");
        } catch (InterruptedException e) {
            System.out.println("was interrupted");
        }
        //抛出异常后，会清除中断标志，这里会返回false
        System.out.println("Point D: t.isInterrupted()=" + t.isInterrupted());
    }
}
```
运行结果如下：
```
Point A: t.isInterrupted()=false
Point B: t.isInterrupted()=true
Point C: t.isInterrupted()=true
was interrupted
Point D: t.isInterrupted()=false
```

### 使用 Thread.interrupted()方法判断中断状态
可以使用 Thread.interrupted()方法来检查当前线程的中断状态（并隐式重置为 false）。又由于它是静态方法，因此不能在特定的线程上使用，而只能报告调用它的线程的中断状态，如果线程被中断，而且中断状态尚不清楚，那么，这个方法返回 true。
与 isInterrupted()不同，它将自动重置中断状态为 false，第二次调用 Thread.interrupted()方法，总是返回 false，除非中断了线程。
```
public class InterruptReset {
    public static void main(String[] args) {
        System.out.println(
                "Point X: Thread.interrupted()=" + Thread.interrupted());
        Thread.currentThread().interrupt();
        System.out.println(
                "Point Y: Thread.interrupted()=" + Thread.interrupted());
        System.out.println(
                "Point Z: Thread.interrupted()=" + Thread.interrupted());
    }
}
```
运行结果如下：
```
Point X: Thread.interrupted()=false
Point Y: Thread.interrupted()=true
Point Z: Thread.interrupted()=false
```
从结果中可以看出，当前线程中断自身后，在 Y 点，中断状态为 true，并由 Thread.interrupted()自动重置为 false，那么下次调用该方法得到的结果便是 false。

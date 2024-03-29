package com.deng.product_consumer;

/**
 * 用synchronized 实现
 * 线程之间的通信问题：生产者和消费者问题
 * 线程交替执行 A B操作同一个变量 num=0
 * A: num+1
 * B: num-1
 */

public class A {
    public static void main(String[] args) {
        Data data = new Data();
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    data.increment();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "A").start();

        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    data.decrement();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "B").start();

        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    data.increment();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "C").start();

        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    data.decrement();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "D").start();
    }
}

//资源类
//判断等待，业务，通知
class Data {
    private int number = 0;

    //+1
    public synchronized void increment() throws InterruptedException {
        //if 会出现虚假唤醒
        while (number != 0) {
            //等待 并且释放当前锁
            this.wait();
        }
        number++;
        System.out.println(Thread.currentThread().getName() + "=>" + number);
        //通知其他线程 我+1完毕 告诉其他需要获取此锁资源的线程我已经执行完毕了
        this.notifyAll();
    }

    //-1
    public synchronized void decrement() throws InterruptedException {
        while (number == 0) {
            //等待
            this.wait();
        }
        number--;
        System.out.println(Thread.currentThread().getName() + "=>" + number);
        //通知其他线程 我-1完毕
        this.notifyAll();
    }
}
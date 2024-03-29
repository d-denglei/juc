package com.deng.volatile_test;

/**
 * @author DengLei
 * @date 2023/04/11 17:22
 */

//不保证原子性
public class JMMDemo02 {

    private volatile static int num = 0;

    public static void add() {
        num++;
    }

    public static void main(String[] args) {
        //通过使用信号量 控制也可以打到效果 类似加锁
//        Semaphore semaphore = new Semaphore(1);
        //理论上num结果是2万
        for (int i = 0; i < 20; i++) {
            new Thread(() -> {
//                for (int j = 0; j < 1000; j++) {
//                    try {
////                        semaphore.acquire();
//                        add();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    } finally {
//                        semaphore.release();
//                    }
//                }
                for (int j = 0; j < 1000; j++) {
                    add();
                }
            }).start();
        }

        while (Thread.activeCount() > 2) { //main  gc
            Thread.yield();
        }
        System.out.println(Thread.currentThread().getName() + " " + num);
    }
}

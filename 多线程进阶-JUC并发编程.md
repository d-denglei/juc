## **多线程进阶->JUC并发编程**

​	

## 1、什么是JUC

**业务:普通的的线程代码 Thread**

**Runnable** 没有返回值、**效率**相比Callable相对较低

## 2、线程和进程

进程:一个程序，例如：QQ.exe,WECHAT.exe

一个进程包含多个线程，至少包含一个线程！

**Java默认有几个线程？2个：main线程、GC**

线程：比如当前开启的typora进程，然后写入内容自动保存（ 通过线程去负责）

对于Java而言：Thread、Runnable、Callabe

**Java是否可以开启线程么？**不可以



```java
    public synchronized void start() {
        /**
         * This method is not invoked for the main method thread or "system"
         * group threads created/set up by the VM. Any new functionality added
         * to this method in the future may have to also be added to the VM.
         *
         * A zero status value corresponds to state "NEW".
         */
        if (threadStatus != 0)
            throw new IllegalThreadStateException();

        /* Notify the group that this thread is about to be started
         * so that it can be added to the group's list of threads
         * and the group's unstarted count can be decremented. */
        group.add(this);

        boolean started = false;
        try {
            start0();
            started = true;
        } finally {
            try {
                if (!started) {
                    group.threadStartFailed(this);
                }
            } catch (Throwable ignore) {
                /* do nothing. If start0 threw a Throwable then
                  it will be passed up the call stack */
            }
        }
    }
	
	// 本地方法,底层的C++ java无法直接操作硬件
    private native void start0();
```



并发编程：并发、并行

并发：多个线程操作同一个资源

- ​	CPU 一核,模拟出来多条线程，是因为速度太快，看起来像是同时操作

  ```
  package com.deng.demo01;
  
  
  public class Test1 {
      public static void main(String[] args) {
          //获取cpu核数
          //CPU 密集型,IO密集型
          System.out.println(Runtime.getRuntime().availableProcessors());
      }
  }
  ```

  **并发编程的本质：充分利用CPU的资源**

并行：多个程序同时执行



线程有几个状态？

```java
public enum State {
		//新建
        NEW,
		//运行
        RUNNABLE,
		//阻塞
        BLOCKED,
		//等待，死死等待
        WAITING,
		//超时等待
        TIMED_WAITING,
		//中止
        TERMINATED;
    }
```



**wait跟sleep的区别是？**

**1、来自不同的类**

wait=>Object

sleep=>Thread

**2、关于锁的释放**

wait会释放锁，sleep是抱着锁睡觉，不会释放！

**3、使用的范围是不同的**

wait:只能在同步代码块中使用(需要先获取锁 才知道释放的锁)

sleep:可以在任何地方使用

**4、是否需要捕获异常**

wait不需要捕获异常 

sleep需要捕获一个超时等待异常

 都需要捕获一个 中断等待异常

## 3、Lock锁（重点）

>在大多数情况下，应使用以下惯用语：

```JAVA
   Lock l = ...;
	l.lock(); 
	try { 
        // access the resource protected by this lock 		} finally {
        l.unlock();
    } 
```

当在不同范围内发生锁定和解锁时，必须注意确保在锁定时执行的所有代码由try-finally或try-catch保护，以确保在必要时释放锁定。

有三个实现类 

**ReentrantLock:可重入锁(常用)**

--首先ReentrantLock里面有三个类主要Sync继承自AQS类，然后还有一个非公平锁跟公平锁 继承自Sync类

AQS中维护了一个先进先出的等待队列用来记录等待的线程队列里面存放的是node节点，node中有个state状态表示表示当前线程状态状态

非公平锁跟公平锁区别在于当点lock的时候，非公平锁不会等待 会先去抢占一次锁，抢占失败会通过tryAcquire再次抢占一次锁，如果两次锁都没抢占成功那么就会把当前线程放入队列中，

公平锁，最开始抢占锁的时候会去判断当前是否存在等待时间更长的线程，如果存在的话那么就会加入队列尾部实现公平获取原则



**ReentrantReadWriteLock.ReadLock:读锁**

**ReentrantReadWriteLock.WriteLock:写锁**

```JAVA

	//公平锁
    public ReentrantLock() {
        sync = new NonfairSync();
    }

	//非公平锁
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }
```



**公平锁：**不允许插队，必须等待完毕

**非公平锁（java默认）**：十分不公平 可以允许插队(默认)

```JAVA
package com.deng.demo01;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SaleTicketDemo02 {
    public static void main(String[] args) {
        //并发:多个线程操作同一个资源， 把资源类丢入线程
        Ticket2 ticket2 = new Ticket2();

        //@FunctionalInterface 匿名内部类 jdk1.8 lambda表达式 (参数)->{具体代码}
        new Thread(() -> {
            for (int i = 1; i < 40; i++) {
                ticket2.sale();
            }
        }, "A").start();

        new Thread(() -> {
            for (int i = 1; i < 40; i++) {
                ticket2.sale();
            }
        }, "B").start();

        new Thread(() -> {
            for (int i = 1; i < 40; i++) {
                ticket2.sale();
            }
        }, "C").start();
    }
}

//lock
//资源类
class Ticket2 {

    //票数
    private int number = 30;

    //官方文档 使用常用语法
    // Lock l = ...;
    // l.lock();
    // try { // access the resource protected by this lock }
    // finally { l.unlock(); }

    Lock lock = new ReentrantLock();

    //卖票的方式
    public void sale() {
        lock.lock();
        try {
            if (number > 0) {
                System.out.println(Thread.currentThread().getName()
                        + "卖出了" + (number--) + "票:剩余：" + number);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();//解锁
        }

    }
}
```

### Synchronized跟Lock的区别？

1、Synchorized	是内置的关键字； Lock是接口类

2、Synchorized	无法判断锁状态；Lock可以判断是否获取到了锁

3、Synchorized	会自动释放锁；Lock加锁解锁都是手动释放！如果不释放锁，会出现**死锁**

4、Synchorized	线程1获取锁， 线程二(等待) 当线程一阻塞，线程二会一直等待；Lock不一定会一直等待(tryLock 尝试获取锁)

5、Synchorized	可重入锁，不可以中断的，非公平；Lock，可重入锁，可以判断锁，可自级设置是否非公平(构造方法)

6、Synchorized	适合锁少量的代码同步问题，Lock 适合锁大量的同步代码！



### 问题： 锁是什么？如何判断锁的是谁？

把单例模式，排序算法，死锁，生产者和消费者 手写伪代码出来



## 4、生产者和消费者的问题

### 用Synchorized 实现

```JAVA
package com.deng.product_consumer;

/**
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
        if (number != 0) {
            //等待
            this.wait();
        }
        number++;
        System.out.println(Thread.currentThread().getName() + "=>" + number);
        //通知其他线程 我+1完毕
        this.notifyAll();
    }

    //-1
    public synchronized void decrement() throws InterruptedException {
        if (number == 0) {
            //等待
            this.wait();
        }
        number--;
        System.out.println(Thread.currentThread().getName() + "=>" + number);
        //通知其他线程 我-1完毕
        this.notifyAll();
    }
}
```

 

> 数字异常问题: 有A、B两个线程的时候 发现并不会出现一次的问题会正常执行，但是当出现 A、B、C、 D四个线程的时候会出现数字异常的问题！线程的虚假唤醒(理解：当一个条件满足时，很多线程都被唤醒了，但是只需要部分线程唤醒，其他线程不需要唤醒导致)

造成问题的原因是：当num = 0时候，AC线程同时执行，A线程未进入if判断 直接+1，通知其他线程，C线程这个时候执行 进入if判断，条件成立 释放当前锁，并且等待，这个时候如果B线程或者D线程执行 -1操作并且成功，那么当前number =0  此时C线程还在等待 当A线程继续执行 然后+1通知正在等待的C线程 此时 number = 1 并且C线程已经走进入if语句 所以不会再判断，直接往下走，number再+1 =2.

**总结：用if判断 wait方法醒来之后不会继续判断当前条件而是直接往下走，如果用while 方法即使wait醒来以后还会再继续判断当前条件不成立那么会继续wait**

下面是JDK1.8源码文档解释

```
public final void wait()
                throws InterruptedException
导致当前线程等待，直到另一个线程调用该对象的notify()方法或notifyAll()方法。 换句话说，这个方法的行为就好像简单地执行呼叫wait(0) 。
当前的线程必须拥有该对象的显示器。 该线程释放此监视器的所有权，并等待另一个线程通知等待该对象监视器的线程通过调用notify方法或notifyAll方法notifyAll 。 然后线程等待，直到它可以重新获得监视器的所有权并恢复执行。

像在一个参数版本中，中断和虚假唤醒是可能的，并且该方法应该始终在循环中使用：

  synchronized (obj) {
         while (<condition does not hold>)
             obj.wait();
         ... // Perform action appropriate to condition
     } 
该方法只能由作为该对象的监视器的所有者的线程调用。 有关线程可以成为监视器所有者的方式的说明，请参阅notify方法。
异常
IllegalMonitorStateException - 如果当前线程不是对象监视器的所有者。
InterruptedException - 如果任何线程在当前线程等待通知之前或当前线程中断当前线程。 当抛出此异常时，当前线程的中断状态将被清除。
另请参见：
notify() ， notifyAll()
```

把if 改成while 可以使得 ABCD四个线程就正常了





Synchronized -> wait等待 notify 通知

Lock -> await 等待   signal 通知

### 用JUC实现

```JAVA
一个Condition实例本质上绑定到一个锁。 要获得特定Condition实例的Condition实例，请使用其newCondition()方法。

例如，假设我们有一个有限的缓冲区，它支持put和take方法。 如果在一个空的缓冲区尝试一个take ，则线程将阻塞直到一个项目可用; 如果put试图在一个完整的缓冲区，那么线程将阻塞，直到空间变得可用。 我们希望在单独的等待集中等待put线程和take线程，以便我们可以在缓冲区中的项目或空间可用的时候使用仅通知单个线程的优化。 这可以使用两个Condition实例来实现。

  class BoundedBuffer {
   final Lock lock = new ReentrantLock();
   final Condition notFull  = lock.newCondition(); 
   final Condition notEmpty = lock.newCondition(); 

   final Object[] items = new Object[100];
   int putptr, takeptr, count;

   public void put(Object x) throws InterruptedException {
     lock.lock(); try {
       while (count == items.length)
         notFull.await();
       items[putptr] = x;
       if (++putptr == items.length) putptr = 0;
       ++count;
       notEmpty.signal();
     } finally { lock.unlock(); }
   }

   public Object take() throws InterruptedException {
     lock.lock(); try {
       while (count == 0)
         notEmpty.await();
       Object x = items[takeptr];
       if (++takeptr == items.length) takeptr = 0;
       --count;
       notFull.signal();
       return x;
     } finally { lock.unlock(); }
   }
 } 
```



## 5、如何判断锁的是谁？8锁现象

如何判断到底是锁谁？本质就是 锁对象 或者锁类class

```JAVA
package com.deng.lock8;


import java.util.concurrent.TimeUnit;

/**
 * 八锁问题
 * 1、标准情况下，两个线程先打印 发短信还是打电话？ 1发短信 2打电话
 * 2、sendSms延迟4秒，两个线程先打印 发短信还是打电话？ 1发短信 2打电话
 */
public class Test1 {
    public static void main(String[] args) {
        Phone phone = new Phone();
        new Thread(() -> {
            phone.sendSms();
        }, "A").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            phone.call();
        }, "B").start();

    }
}

class Phone {

    //synchronized 锁的是对象是方法的调用者
    //两个方法用的是同一个锁，谁先拿到谁就执行！
    public synchronized void sendSms() {

        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("发短信");
    }

    public synchronized void call() {
        System.out.println("打电话");
    }
}

```



```JAVA
package com.deng.lock8;

import java.util.concurrent.TimeUnit;


/**
 * 八锁问题
 * 3、增加了一个普通方法 hello 输出发短信还是hello? 1hello 2发短信
 * 4、两个对象,两个同步方法 先输出打电话还是发短信？  1打电话 2发短信
 */
public class Test2 {
    public static void main(String[] args) {
        //两个对象
        Phone2 phone1 = new Phone2();
        Phone2 phone2 = new Phone2();

        new Thread(() -> {
            phone1.sendSms();
        }, "A").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            phone2.call();
        }, "B").start();

    }
}

class Phone2 {

    //synchronized 锁的是对象是方法的调用者
    //两个方法用的是同一个锁，谁先拿到谁就执行！
    public synchronized void sendSms() {

        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("发短信");
    }

    public synchronized void call() {
        System.out.println("打电话");
    }

    //这里没有锁,不是同步方法,不受锁的影响
    public void hello() {
        System.out.println("hello");
    }
}

```



```JAVA
package com.deng.lock8;

import java.util.concurrent.TimeUnit;

/**
 * 八锁问题
 * 5、增加两个静态的同步方法，只有一个对象 先打印发短信还是打电话？ 1发短信2打电话
 * 6、两个对象，两个静态方法 会先发短信还是打电话？  1发短信 2打电话
 */
public class Test3 {
    public static void main(String[] args) {
        //两个对象的Class类模板只有一个 锁定的是同一个class
        Phone3 phone1 = new Phone3();
        Phone3 phone2 = new Phone3();

        new Thread(() -> {
            phone1.sendSms();
        }, "A").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            phone2.call();
        }, "B").start();

    }
}

class Phone3 {

    //synchronized 锁的是对象是方法的调用者
    //static 修饰以后 静态方法
    //类初始化加载就存在了！锁的是类class对象
    public static synchronized void sendSms() {
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("发短信");
    }

    public static synchronized void call() {
        System.out.println("打电话");
    }
}
```



```JAVA
package com.deng.lock8;

import java.util.concurrent.TimeUnit;

/**
 * 八锁问题
 * 7、1个静态同步方法 1个普通同步方法，先打印发短信还是打电话？  1打电话 2发短信
 * 8、两个对象分别调用 一个static 修饰的同步方法  一个普通同步方法，先打印哪个?  1打电话 2发短信
 */
public class Test4 {
    public static void main(String[] args) {
        //两个对象的Class类模板只有一个 锁定的是同一个class
        Phone4 phone1 = new Phone4();
        Phone4 phone2 = new Phone4();

        new Thread(() -> {
            phone1.sendSms();
        }, "A").start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            phone2.call();
        }, "B").start();

    }
}

class Phone4 {

    //synchronized 锁的是对象是方法的调用者
    //static 修饰以后 静态方法
    //类初始化加载就存在了！锁的是类class对象
    public static synchronized void sendSms() {
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("发短信");
    }

    //锁的调用者
    public synchronized void call() {
        System.out.println("打电话");
    }
}
```



### 八锁小结：

new this 锁的是当前调用对象

static 锁的是类 Class模板对象



## 6、集合类不安全

```JAVA
package com.deng.unsafe;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class ListTest {
    public static void main(String[] args) {
        //并发下 ArrayList 是不安全的
        /**
         * 解决方案：
         * 1. List<String> list = new Vector<>();
         * 2. List<String> list = Collections.synchronizedList(new ArrayList<>());
         * 3. List<String> list = new CopyOnWriteArrayList<>();
         */
        //CopyOnWrite 写入时复制
        //多个线程调用,读取是固定的，写入的时候避免覆盖造成数据问题
        //读写分离 写入的时候复制一份然后将指向原数组地址指向新数组地址
        List<String> list = new CopyOnWriteArrayList<>();

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                list.add(UUID.randomUUID().toString().substring(0, 5));
                System.out.println(list);
            }, String.valueOf(i)).start();
        }
    }
}

```



CopyOnWriteArraylist 底层维护了一个Reentranklock 可冲入锁跟一个被voliate 修饰的数组

一般用户多读少写，写入时候复制一份原有数据并且新增数据到新集合中然后替换原有数据引用地址



```JAVA
package com.deng.unsafe;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MapTest {
    public static void main(String[] args) {
        //map 日常工作如果要用map切保证安全 尽量用ConCurrentHashMap
        //默认等价于   new HashMap<>(16,0.75f)
//        HashMap<String, String> map = new HashMap<>();
        Map<String, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                map.put(Thread.currentThread().getName(), UUID.randomUUID().toString().substring(0, 5));
                System.out.println(map);
            }).start();
        }
    }
}

```

## 7、Callable



```
@FunctionalInterface
public interface Callable<V>
返回结果并可能引发异常的任务。 实现者定义一个没有参数的单一方法，称为call 。
Callable接口类似于Runnable ，因为它们都是为其实例可能由另一个线程执行的类设计的。 然而，A Runnable不返回结果，也不能抛出被检查的异常。

该Executors类包含的实用方法，从其他普通形式转换为Callable类。
```

1、可以有返回值

2、可以抛出异常

3、方法不同 普通thread 或者runnalbe是 run()方法入口，这个是call().

```JAVA
package com.deng.callable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author DengLei
 * @date 2023/03/21 10:54
 */

public class CallableTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //new Thread(new  Runnable()).start();
        //new Thread(new  MyThread()).start();
        //等价于 new Thread(new FutureTask<V>( Callable  )).start();
        new Thread().start();//如何启动Callable

        MyThread myThread = new MyThread();
        FutureTask<String> futureTask = new FutureTask<>(myThread);
        //添加适配类绑定到Thread中

        new Thread(futureTask, "A").start();
        new Thread(futureTask, "B ").start();
        String o = futureTask.get();//get方法可能会产生阻塞 会等待线程返回结果
        System.out.println(o);

    }
}

class MyThread implements Callable<String> {

    @Override
    public String call() throws Exception {
        System.out.println("call()");
        return "123456";
    }
}


```

##### 细节：

1、同一个futuretask多次执行的话只会获取第一个结果，根据源码执行run方法会去判断 state如果state状态不是新建状态0就会直接返回

2、 结果可能会需要等待，会阻塞！

```JAVA
 public void run() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex);
                }
                if (ran)
                    set(result);
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }
```



## 8、JUC下常用辅助类

### 	8.1、CountDownLatch



```JAVA
package com.deng.add;

import java.util.concurrent.CountDownLatch;

/**
 * 计数器 -1
 *
 * @author DengLei
 * @date 2023/03/21 15:25
 */
public class CountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
        //总数是6 必须要等6执行任务的时候再使用!
        CountDownLatch countDownLatch = new CountDownLatch(6);
        for (int i = 1; i <= 6; i++) {
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName() + "Go out");
                countDownLatch.countDown();//数量-1
            }, String.valueOf(i)).start();
        }
        countDownLatch.await();//等待计数器归零，然后继续操作
        System.out.println("close Door");
    }
}

```

##### 原理：

countDownLatch.countDown()  //数量-1

countDownLatch.await(); //等待计数器归零，然后再向下执行

每次有线程调用countDown 就把数量-1，假设计数器为0，那么

countDownLatch.await() 就会被唤醒继续执行

### 	8.2、CycliBarrier

```JAVA
package com.deng.add;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * 计数器 +1
 *
 * @author DengLei
 * @date 2023/03/21 15:48
 */

public class CyclicBarrierDemo {
    public static void main(String[] args) {
        /**
         * 集齐七颗龙珠召唤神龙 线程
         */
        //召唤龙珠线程
        CyclicBarrier cyclicBarrier = new CyclicBarrier(6, () -> {
            System.out.println("召唤神龙成功！");
        });


        for (int i = 1; i <= 6; i++) {

            final int temp = i;
            //lambda 无法操作匿名内部类外面的值
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName() + "收集了" + temp + "龙珠");
                try {
                    cyclicBarrier.await();//等待
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}

```



##### CountDownLatch跟CycliBarrier区别：

CountDownLatch只要阻塞足够多的次数就够了，不需要管几个线程执行，如果一个线程多次执行countdown也可以满足。

CycliBarrier  线程之间会等待 只有所有线程都执行了await才会继续往下走

### 	8.3、Semaphore

```java
public class Semaphore
extends Object
implements Serializable
一个计数信号量。 在概念上，信号量维持一组许可证。 如果有必要，每个acquire()都会阻塞，直到许可证可用，然后才能使用它。 每个release()添加许可证，潜在地释放阻塞获取方。 但是，没有使用实际的许可证对象; Semaphore只保留可用数量的计数，并相应地执行。
信号量通常用于限制线程数，而不是访问某些（物理或逻辑）资源。 例如，这是一个使用信号量来控制对一个项目池的访问的类：

   class Pool { private static final int MAX_AVAILABLE = 100; private final Semaphore available = new Semaphore(MAX_AVAILABLE, true); public Object getItem() throws InterruptedException { available.acquire(); return getNextAvailableItem(); } public void putItem(Object x) { if (markAsUnused(x)) available.release(); } // Not a particularly efficient data structure; just for demo protected Object[] items = ... whatever kinds of items being managed protected boolean[] used = new boolean[MAX_AVAILABLE]; protected synchronized Object getNextAvailableItem() { for (int i = 0; i < MAX_AVAILABLE; ++i) { if (!used[i]) { used[i] = true; return items[i]; } } return null; // not reached } protected synchronized boolean markAsUnused(Object item) { for (int i = 0; i < MAX_AVAILABLE; ++i) { if (item == items[i]) { if (used[i]) { used[i] = false; return true; } else return false; } } return false; } } 
在获得项目之前，每个线程必须从信号量获取许可证，以确保某个项目可用。 当线程完成该项目后，它将返回到池中，并将许可证返回到信号量，允许另一个线程获取该项目。 请注意，当调用acquire()时，不会保持同步锁定，因为这将阻止某个项目返回到池中。 信号量封装了限制对池的访问所需的同步，与保持池本身一致性所需的任何同步分开。

信号量被初始化为一个，并且被使用，使得它只有至多一个允许可用，可以用作互斥锁。 这通常被称为二进制信号量 ，因为它只有两个状态：一个许可证可用，或零个许可证可用。 当以这种方式使用时，二进制信号量具有属性（与许多Lock实现不同），“锁”可以由除所有者之外的线程释放（因为信号量没有所有权概念）。 这在某些专门的上下文中是有用的，例如死锁恢复。

此类的构造函数可选择接受公平参数。 当设置为false时，此类不会保证线程获取许可的顺序。 特别是， 闯入是允许的，也就是说，一个线程调用acquire()可以提前已经等待线程分配的许可证-在等待线程队列的头部逻辑新的线程将自己。 当公平设置为真时，信号量保证调用acquire方法的线程被选择以按照它们调用这些方法的顺序获得许可（先进先出; FIFO）。 请注意，FIFO排序必须适用于这些方法中的特定内部执行点。 因此，一个线程可以在另一个线程之前调用acquire ，但是在另一个线程之后到达排序点，并且类似地从方法返回。 另请注意， 未定义的tryAcquire方法不符合公平性设置，但将采取任何可用的许可证。
```



```JAVA
package com.deng.add;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author DengLei
 * @date 2023/03/22 16:11
 */
//模拟停车位  6个车 三个车位 1一个车位空出来了就进去一个车
public class SemaphoreDemo {
    public static void main(String[] args) {
        //线程数量：停车位 //做限流可以用到这个
        //Semaphore的参数代表资源数
        Semaphore semaphore = new Semaphore(3);
        for (int i = 1; i <= 6; i++) {
            new Thread(() -> {
                //acquire() 得到车位
                try {
                    semaphore.acquire();
                    System.out.println(Thread.currentThread().getName() + "抢到车位");
                    TimeUnit.SECONDS.sleep(2);
                    //停了两秒后 离开
                    System.out.println(Thread.currentThread().getName() + "离开车位");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    //释放
                    semaphore.release();
                }

            }, String.valueOf(i)).start();
        }

    }
}

```



##### 原理:

​	semaphore.acquire() 获取资源，假设如果已经满了，那么会等待，知道资源被释放

​	semaphore.release() 释放，会将当前的信号量-1，然后唤醒等待的线程！作用：多个共享资源互斥的使用！并发限流,控制最大的线程数！

##### semaphore 四个场景问题

###### 1、semaphore初始化有10个令牌-11个线程同时各调用1次acquire方法-会发生什么？

​	答案：拿不到令牌的线程阻塞，不会继续往下运行。

###### 2、semaphore初始化有10个令牌-一个线程重复调用11次acquire方法-会发生什么?

​	答案：线程阻塞，不会继续往下运行。可能你会考虑类似于锁的重入的问题，很好，但是，令牌没有重入的概念。你只要调用一次acquire方法，就需要有一个令牌才能继续运行。

###### 3、semaphore初始化有1个令牌-1个线程调用一次acquire方法-然后调用两次release方法-之后另外一个线程调用acquire-2-方法-此线程能够获取到足够的令牌并继续运行吗?

​	答案：能，原因是release方法会添加令牌，并不会以初始化的大小为准。

###### 4、semaphore初始化有2个令牌，一个线程调用1次release方法，然后一次性获取3个令牌，会获取到吗?

​	答案：能，原因是release会添加令牌，并不会以初始化的大小为准。Semaphore中release方法的调用并没有限制要在acquire后调用。



## 9、读写锁：ReadWriteLock



```JAVA
package com.deng.read_write_lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 独占锁(写锁) 一次只能一个线程占有
 * 共享锁(读锁) 多个线程可以同时占有
 * ReadWriteLock
 * 读-读 共存！
 * 读-写 不共存！
 * 写-写 不共存！
 */
public class ReadWriteLockDemo {

    public static void main(String[] args) {
//        MyCache myCache = new MyCache();
        MyCacheLock myCache = new MyCacheLock();
        for (int i = 1; i <= 10; i++) {
            final int temp = i;
            new Thread(() -> {
                myCache.put(temp + "", temp + "");
            }, String.valueOf(i)).start();
        }
        for (int i = 1; i <= 10; i++) {
            final int temp = i;
            new Thread(() -> {
                myCache.get(temp + "");
            }, String.valueOf(i)).start();
        }

    }
}

class MyCache {

    private volatile Map<String, Object> map = new HashMap<>();

    //存,写
    public void put(String key, Object value) {
        System.out.println(Thread.currentThread().getName() + "写入" + key);
        map.put(key, value);
        System.out.println(Thread.currentThread().getName() + "写入ok");
    }

    //取,读
    public void get(String key) {
        System.out.println(Thread.currentThread().getName() + "读取" + key);
        Object o = map.get(key);
        System.out.println(Thread.currentThread().getName() + "读取ok");

    }
}

//加锁的
class MyCacheLock {
    private volatile Map<String, Object> map = new HashMap<>();
    //加读写锁:更加细粒度的控制
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private ReentrantLock lock = new ReentrantLock();

    //存,写
    public void put(String key, Object value) {
        readWriteLock.writeLock().lock();

        try {
            System.out.println(Thread.currentThread().getName() + "写入" + key);
            map.put(key, value);
            System.out.println(Thread.currentThread().getName() + "写入ok");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            readWriteLock.writeLock().unlock();
        }

    }

    //如果读不加锁的话 可能会造成幻读问题 开始读到一个内容 然后读取到另外一个内容
    //取,读
    public void get(String key) {
        readWriteLock.readLock().lock();
        try {
            System.out.println(Thread.currentThread().getName() + "读取" + key);
            Object o = map.get(key);
            System.out.println(Thread.currentThread().getName() + "读取ok");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
}
```



## 10、1、阻塞队列

```JAVA
Interface Queue<E>
参数类型
E - 保存在此集合中的元素的类型
All Superinterfaces:
Collection <E>， Iterable <E>
All Known Subinterfaces:
BlockingDeque <E>， BlockingQueue <E>， Deque <E>//双端队列， TransferQueue <E>
所有已知实现类：
AbstractQueue ， ArrayBlockingQueue ， ArrayDeque ， ConcurrentLinkedDeque ， ConcurrentLinkedQueue ， DelayQueue ， LinkedBlockingDeque ， LinkedBlockingQueue ， LinkedList ， LinkedTransferQueue ， PriorityBlockingQueue ， PriorityQueue ， SynchronousQueue//同步队列
```

 

#### 如何使用队列？

 四组API

| 方式       | 抛出异常  | 不会抛出异常，有返回值 | 阻塞等待 | 超时等待        |
| ---------- | --------- | ---------------------- | -------- | --------------- |
| 添加       | add()     | offer()                | put()    | offer(等待时间) |
| 移除       | remove()  | poll()                 | take()   | poll(等待时间)  |
| 获取队列首 | element() | peek()                 | -        | -               |



```JAVA
    /**
     * 抛出异常
     */
    public static void test1() {
        //队列的大小
        ArrayBlockingQueue blockingQueue = new ArrayBlockingQueue(3);
        //进队 add
        System.out.println(blockingQueue.add("a"));
        System.out.println(blockingQueue.add("b"));
        System.out.println(blockingQueue.add("c"));

        //java.util.IllegalStateException: Queue full 队列已满异常
//        System.out.println(blockingQueue.add("e"));

        //出队 remove
        System.out.println(blockingQueue.remove());
        //查看对首元素是谁
        System.out.println(blockingQueue.element());
        System.out.println(blockingQueue.remove());
        System.out.println(blockingQueue.remove());
        //java.util.NoSuchElementException 队列中没有元素异常！
//        System.out.println(blockingQueue.remove());
    }

```



```JAVA
      /**
     * 不抛出异常
     */
    public static void test2() {
        //队列的大小
        ArrayBlockingQueue blockingQueue = new ArrayBlockingQueue(3);
        System.out.println(blockingQueue.offer("a"));
        System.out.println(blockingQueue.offer("b"));
        System.out.println(blockingQueue.offer("c"));

        //如果队列满了 就加入不进去  false 不抛出异常
//        System.out.println(blockingQueue.offer("d"));

        //检测队首元素
//        System.out.println(blockingQueue.peek());
//        System.out.println(blockingQueue.element());
        System.out.println(blockingQueue.poll());
        System.out.println(blockingQueue.poll());
        System.out.println(blockingQueue.poll());
        //如果队列空了则 不返回null
        System.out.println(blockingQueue.peek());
    }
```



```
    /**
     * 等待,阻塞(一直阻塞)
     */
    public static void test3() throws InterruptedException {
        //队列的大小
        ArrayBlockingQueue blockingQueue = new ArrayBlockingQueue(3);
        //一直阻塞
        blockingQueue.put("a");
        blockingQueue.put("b");
        blockingQueue.put("c");
//        blockingQueue.put("d"); //阻塞存, 队列没位置了,进入一直等待阻塞中
        System.out.println(blockingQueue.take());
        System.out.println(blockingQueue.take());
        System.out.println(blockingQueue.take());
//        System.out.println(blockingQueue.take());//阻塞取 当没存在元素会一直阻塞
    }
```



```JAVA
  /**
     * 等待,阻塞(超时等待)
     */
    public static void test4() throws Exception {
        //队列的大小
        ArrayBlockingQueue blockingQueue = new ArrayBlockingQueue(3);
        System.out.println(blockingQueue.offer("a"));
        System.out.println(blockingQueue.offer("b"));
        System.out.println(blockingQueue.offer("c"));
        //等待超时,如果超出时间就会直接超时返回 false
        System.out.println(blockingQueue.offer("d", 1, TimeUnit.SECONDS));

        System.out.println(blockingQueue.poll());
        System.out.println(blockingQueue.poll());
        System.out.println(blockingQueue.poll());
        //超时等待，如果从队列中拿不到元素 会等待两秒后返回null
        System.out.println(blockingQueue.poll(2, TimeUnit.SECONDS));
    }
```



#### 	2、同步对列 

###### 	SynchronousQueue 

​	介绍:没有容量，进去一个元素，必须等待取出以后，才能再放入一个元素！ put，take



```JAVA
package com.deng.block_queue;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * 同步队列
 * 和其他的BlockingQueue不一样 ，SynchronousQueue不存储元素
 * put了一个元素,必须从里面先take取出来，否则不能再put进去值！
 */

public class SynchronousQueueDemo {
    public static void main(String[] args) {
        //同步队列
        BlockingQueue<String> blockingQueue = new SynchronousQueue<>();

        new Thread(() -> {
            try {
                System.out.println(Thread.currentThread().getName() + " put 1");
                blockingQueue.put("1");
                System.out.println(Thread.currentThread().getName() + " put 2");
                blockingQueue.put("2");
                System.out.println(Thread.currentThread().getName() + " put 3");
                blockingQueue.put("3");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "T1").start();

        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(3);
                System.out.println(Thread.currentThread().getName() + "=>" + blockingQueue.take());
                TimeUnit.SECONDS.sleep(3);
                System.out.println(Thread.currentThread().getName() + "=>" + blockingQueue.take());
                TimeUnit.SECONDS.sleep(3);
                System.out.println(Thread.currentThread().getName() + "=>" + blockingQueue.take());


            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "T2").start();
    }
}

```



## 11、线程池(重点)

##### 池化技术：

程序的运行，本质：占用系统资源！优化资源使用 => 池化技术！

例如 ：线程池，内存池，常量池，对象池... 创建销毁浪费资源

池化技术：提前准备一些资源，需要用就直接去拿，用完归还



线程池的好处：

1、降低资源的消耗

2、提交响应速度

3、方便 管理

线程复用，可以控制最大并发数，管理线程

##### <font color='red'>线程池几个要点：三大方法、7大参数、4种拒绝策略</font>



###### 三大方法：

```JAVA
package com.deng.thread_pool;


import java.util.concurrent.*;

/**
 * Executors 工具类 三大方法
 * new ThreadPoolExecutor.AbortPolicy()  //默认的拒绝策略  抛出异常
 * new ThreadPoolExecutor.CallerRunsPolicy()   拿来的去哪里
 * new ThreadPoolExecutor.DiscardPolicy() 队列满了就会丢掉任务 不会抛出异常
 * new ThreadPoolExecutor.DiscardOldestPolicy() 队列满了，尝试竞争最早的线程  没抢到也不会抛出异常
 */
public class Demo01 {
    String a;
    String b;

    public Demo01() {
        this("A", "b");
    }

    public Demo01(String a, String b) {
        this.a = a;
        this.b = b;
    }

    public static void main(String[] args) {


        /**
         * 三大方法
         */
        //单例模式  单线程
//        ExecutorService threadPool = Executors.newSingleThreadExecutor();
//        固定的线程池的大小
//        ExecutorService threadPool = Executors.newFixedThreadPool(5);
//        //可扩建的线程池
//        ExecutorService threadPool = Executors.newCachedThreadPool();

        /**
         * 七大参数  四种拒绝策略
         */
        // 工作中建议用这个方法去创建 用Executors 创建不太安全
        ExecutorService threadPool = new ThreadPoolExecutor(
                2,      //核心线程数
                5,  //最大线程数
                3,      //超时释放时间
                TimeUnit.SECONDS,   //秒
                new LinkedBlockingQueue<>(3),   //阻塞队列大小
                Executors.defaultThreadFactory(),       //默认的线程工厂
                new ThreadPoolExecutor.DiscardOldestPolicy()  //默认的拒绝策略  抛出异常
        );
        try {
            //最大线程数： maxSize + Deque
            for (int i = 0; i < 9; i++) {
                //通过线程池来创建线程
                threadPool.execute(() -> {
                    System.out.println(Thread.currentThread().getName() + " OK");
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //线程池用完接收
            threadPool.shutdown();
        }

    }
}

```



###### 7大参数：

```JAVA
    public static ExecutorService newSingleThreadExecutor() {
        return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>()));
    }

    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());
    }
    public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      60L, TimeUnit.SECONDS,
                                      new SynchronousQueue<Runnable>());
    }

 //实际调用的就是这个方法 七个参数
  public ThreadPoolExecutor(int corePoolSize, //核心线程池大小
                            int maximumPoolSize, //最大线程池大小
                              long keepAliveTime, 	//超时没人调用就释放
                              TimeUnit unit,	//超时单位
                              BlockingQueue<Runnable> workQueue, 	//阻塞对垒
                              ThreadFactory threadFactory,		//线程工厂，创建线程，默认
                              RejectedExecutionHandler handler) { //拒绝策略
        if (corePoolSize < 0 ||
            maximumPoolSize <= 0 ||
            maximumPoolSize < corePoolSize ||
            keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.acc = System.getSecurityManager() == null ?
                null :
                AccessController.getContext();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

```



四种拒绝策略：

```JAVA
RejectedExecutionHandler 的四个实现类  
AbortPolicy   			//默认的拒绝策略  抛出异常
CallerRunsPolicy 		// 拿来的去哪里 比如main方法调用的就让main方法执行
DiscardOldestPolicy 	//队列满了就会丢掉任务 不会抛出异常
DiscardPolicy			//队列满了，尝试竞争最早的线程  没抢到也不会抛出异常
```



###### 线程池小节和拓展：

###### 如何去定义最大线程？ 

###### CPU 密集 IO  密集型（调优）

 1、CPU 密集型  读取几核CPU 就是几 可以保证CPU效率最高

 2、IO  密集型  判断你程序中十分耗IO的线程的个数(一般设置大于两倍)

​			例如-程序中有 15个任务正在进行 io十分占用资源

```JAVA
/**
 * TODO 最大线程该如何去定义？
 * 1、CPU 密集型  读取几核CPU 就是几 可以保证CPU效率最高
 * 2、IO  密集型  判断你程序中十分耗IO的线程的个数(一般设置大于两倍)
 *       例如-程序中有 15个任务正在进行 io十分占用资源
 */
```





## 12、四大函数式接口(必须掌握)



函数式接口：只有一个方法的接口

```
@FunctionalInterface
public interface Runnable {
    public abstract void run();
}
```



###### 1、函数式接口

```JAVA
package com.deng.function;

import java.util.function.Function;

/**
 * Function 函数数接口，有一个输入参数，有一个输出参数
 * 只要是函数型接口 可以用lambda表达式简化
 */
public class Demo01 {
    public static void main(String[] args) {
        // 点开Function<T,R> 发现传入参数 T,返回的类型是R
//        Function function = new Function<String, String>() {
//            @Override
//            public String apply(String str) {
//                return str;
//            }
//        };
        //用lambda写法
//        Function function = (str) ->{
//            return str;
//        };
        //更精简的写法
        Function function = str -> str;

        System.out.println(function.apply("123"));
    }
}
```



###### 2、断定型接口

```JAVA
package com.deng.function;

import java.util.function.Predicate;

/**
 * Predicate断定型接口，输入一个参数 返回值只能是布尔值
 */
public class Demo02 {
    public static void main(String[] args) {
        //传入一个数据返回一个布尔值
        //可用做字符串判断
//        Predicate<String> predicate = new Predicate<String>() {
//            @Override
//            public boolean test(String str) {
//                return str.isEmpty();
//            }
//        };
        Predicate<String> predicate = String::isEmpty;

        System.out.println(predicate.test(""));
    }
}

```



###### 3、消费型接口

```JAVA
package com.deng.function;

import java.util.function.Consumer;

/**
 * Consumer 消费型接口 : 只要输入 没有返回值
 *
 * @author denglei
 * @date 2023/3/27 22:54
 */
public class Demo03 {
    public static void main(String[] args) {
//        Consumer<String> consumer = new Consumer<String>() {
//            @Override
//            public void accept(String str) {
//                System.out.println(str);
//            }
//        };
        Consumer<String> consumer = str -> System.out.println(str);
        consumer.accept("123");
    }
}

```



###### 4、供给型接口

```java
package com.deng.function;

import java.util.function.Supplier;

/**
 *  Supplier 没有参数只有返回值 供给形接口
 *
 * @author denglei
 * @date 2023/3/27 22:58
 */
public class Demo04 {
    public static void main(String[] args) {
        Supplier<Integer> supplier = () -> 1024;
        Integer integer = supplier.get();
        System.out.println(integer);
    }
}

```



## 13.Stream 流计算（已掌握）





## 14、ForkJoin

#### 什么是ForkJoin? 

​	ForkJoin 在JDK1.7存在，并行执行任务！提高效率，**大数据量**！

​	大数据量：Map Reduce  **把大任务拆成多个小任务** 



​					

​						任务

​			子任务   			子任务

子任务	子任务 	子任务    子任务

结果			结果		结果		结果

​					合并为一个最终结果

·



#### ForkJoin特点 ：工作窃取

两个线程AB 如果A线程只执行一半，B线程执行完了，那么会从A线程哪里去拿需要处理的任务	：**ForkJoin因为维护的都是双端队列**



```JAVA
package com.deng.forjoin;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * 求和计算
 * 如何使用forkjoin？
 * 1、使用 forkjoinPool 通过这个执行
 * 2、计算任务 forkjoinPool.execute(ForkJoinTask task)
 */
public class ForkJoinDemo extends RecursiveTask<Long> {

    private Long start;

    private Long end;

    //临界值
    private Long temp = 10000L;

    ForkJoinDemo(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    //具体业务执行
    @Override
    protected Long compute() {
        if ((end - start) > temp) {
            //通过分支合并去计算
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            //中间值
            long middle = (start + end) / 2;

            ForkJoinDemo task1 = new ForkJoinDemo(start, middle);
            task1.fork();//把任务压入线程队列

            ForkJoinDemo task2 = new ForkJoinDemo(middle + 1, end);
            task2.fork();
            //join 阻塞当前线程获取返回结果
            long l = task1.join() + task2.join();
            return l;

        } else {
            long sum = 0L;
            for (Long i = start; i <= end; i++) {
                sum += i;
            }
            return sum;
        }
    }
}

```



```java
package com.deng.forjoin;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.LongStream;

/**
 * @author denglei
 * @date 2023/3/28 22:36
 */
public class ForkJoinTest {
    public static void main(String[] args) throws Exception {
//        test1();
        test2();
        test3();
    }

    //普通计算
    public static void test1() {
        Long sum = 0L;
        long start = System.currentTimeMillis();
        for (Long i = 1L; i <= 10_0000_0000; i++) {
            sum += i;
        }
        long end = System.currentTimeMillis();
        System.out.println("sum =" + sum + " 时间" + (end - start));
    }

    //通过 ForkJoIn
    public static void test2() throws Exception {

        long start = System.currentTimeMillis();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ForkJoinTask<Long> forkJoinDemo = new ForkJoinDemo(0L, 10_0000_0000L);
        forkJoinPool.execute(forkJoinDemo);//提交任务
        long sum = forkJoinDemo.get();


        long end = System.currentTimeMillis();
        System.out.println("sum =" + sum + " 时间" + (end - start));
    }

    //通过Stream的并行流  可以看到用并行流处理速度最快
    public static void test3() {
        long start = System.currentTimeMillis();
        long sum = LongStream.rangeClosed(0L, 10_0000_0000L).parallel().reduce(0, Long::sum);
        long end = System.currentTimeMillis();
        System.out.println("sum =" + sum + " 时间" + (end - start));
    }
}

```





## 15、异步回调FutureTask

**具体可以参考** https://juejin.cn/post/6970558076642394142



**开启==线程==执行任务，不管是使用Runnable(无返回值不支持上报异常)还是Callable(有返回值支持上报异常)接口 都可以轻松实现。**

**但是如何在使用==线程池==的情况下获取结果归集如何实现？**

**Java-多线程 Future、 FutureTask 、CompletionService、CompletableFuture解决多线程中并发归集问题的效率对比！**



|              | Future                                | FutureTask                                                   | CompletionService                                            | CompletableFuture                                            |
| ------------ | ------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
|              | Future接口                            | 接口RunnableFuture的唯一实现类,RunnableFuture继承自Future+Runnable | 内部通过阻塞队列+FutureTask接口                              | JDK8实现了Future<T>,CompletionStage两个接口                  |
| 事物并发执行 | 支持                                  | 支持                                                         | 支持                                                         | 支持                                                         |
| 获取任务结果 | 支持任务完成先后顺序                  | 位置                                                         | 支持任务完成的先后顺序                                       | 支持任务完成的先后顺序                                       |
| 异常捕捉     | 自己捕捉                              | 自己捕捉                                                     | 自己捕捉                                                     | 源生API支持，返回每个任务的异常                              |
|              | Cpu高速轮询，耗资源，可以使用(不推荐) | 功能不对口，并发任务多套一层 不推荐                          | <font color ='red'>推荐使用</font>，在Jdk8CompletableFuture出现之前最好的解决方案 | <font color ='red'>推荐使用,API极端丰富，配合流式编程，速度很快</font> |

Future对于结果的获取，不是很友好，只能通过**阻塞**或者**轮询的方式**得到任务的结果。

- Future.get() 就是阻塞调用，在线程获取结果之前**get方法会一直阻塞**。
- Future提供了一个isDone方法，可以在程序中**轮询这个方法查询**执行结果。

**阻塞的方式和异步编程的设计理念相违背，而轮询的方式会耗费无谓的CPU资源**。因此，JDK8设计出CompletableFuture，

CompletableFuture提供了一种观察者模式类似的机制，可以让**任务执行完成后通知监听的一方。**

CompletableFuture 有几十种方法，辅助异步任务场景，例如 创建异步任务 ，异步任务回调，，多任务组合处理



##### 1、异步执行有返回值跟无返回值

```JAVA
package com.deng.future_task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 异步调用
 */
public class FutureTaskDemo {
    public static void main(String[] args) {
        //可以自定义线程池
        ExecutorService executor = Executors.newCachedThreadPool();
        //runAsync的使用 无返回值
        CompletableFuture<Void> runFuture = CompletableFuture.runAsync(
                () -> System.out.println("runAsync 你好"), executor);

        //supplyAsync的使用 有返回值
        CompletableFuture<String> supplyFuture = CompletableFuture.supplyAsync(
                () -> {
                    try {
                        TimeUnit.SECONDS.sleep(4);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.print("supplyAsync 你好");
                    return "捡田螺的小男孩";
                });
        supplyFuture.thenRunAsync(() -> {
            System.out.println("第二个任务");
        });

        //runAsync的future没有返回值，输出null
        System.out.println("runFuture 结果：" + runFuture.join());

        // supplyFuture.join()也是获取结果值 区别是一个get需要手动处理异常 join不需要 但是碰到RuntimeException也会抛出
        //supplyAsync的future，有返回值
        System.out.println("supplyFuture 结果：" + supplyFuture.join());

        executor.shutdown(); // 线程池需要关闭

    }
}

```

##### 2、thenRun/thenRunAsync

```JAVA
package com.deng.future_task;

import java.util.concurrent.CompletableFuture;

/**
 * thenRun/thenRunAsync 方法:通俗点讲就是做完第一个任务然后去执行第二个任务，可以用
 * 做某个方法执行完以后 的回调方法。前后两个任务没参数传递 第二个任务也没返回值
 */
public class CompletableFutureDemo2 {
    public static void main(String[] args) {
        CompletableFuture<String> orgFuture = CompletableFuture.supplyAsync(
                () -> {
                    System.out.println("先执行第一个CompletableFuture方法任务");
                    return "捡田螺的小男孩";
                }
        );
        /**
         * 调用thenRun方法执行第二个任务时，则第二个任务和第一个任务是共用同一个线程池。
         * 调用thenRunAsync执行第二个任务时，则第一个任务使用的是你自己传入的线程池，第二个任务使用的是ForkJoin线程池
         */
        CompletableFuture<Void> thenRunFuture = orgFuture.thenRunAsync(() -> {
            System.out.println("接着执行第二个任务");
        });

        System.out.println(thenRunFuture.join());
    }
}

```



##### 3、thenAccept 和 thenAcceptAsync

```java
package com.deng.future_task;

import java.util.concurrent.CompletableFuture;

/**
 * thenAccept 和 thenAcceptAsync有什么区别呢？
 * 第一个任务执行完以后，执行第二个回调方法任务，会把第一个任务的执行结果作为入参 传递到第二个任务方法中，
 * 但是回调方法无返回值 两者区别
 */
public class CompletableFutureDemo3 {
    public static void main(String[] args) {
        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("先执行第一个任务");
            return 1024;
        });
        /**
         * 调用thenAccept方法执行第二个任务时，则第二个任务和第一个任务是共用同一个线程池。
         * 调用thenAcceptAsync执行第二个任务时，则第一个任务使用的是你自己传入的线程池，第二个任务使用的是ForkJoin线程池
         */
        completableFuture.thenAcceptAsync(a -> {
            System.out.println("第一个方法执行的内容是 " + a);
        });
        //这里返回的值的内容是第一个方法的值 可见第二个回调方法是没有返回值的
        Integer join = completableFuture.join();
        System.out.println(join);
    }
}

```



##### 4、thenApply/thenApplyAsync

```JAVA
package com.deng.future_task;

import java.util.concurrent.CompletableFuture;

/**
 * thenApply/thenApplyAsync ：
 * 第一个任务执行完成后，执行第二个回调方法任务，并将改任务的执行结果传入回调方法中，回调方法有返回值
 */
public class CompletableFutureDemo4 {
    public static void main(String[] args) {
        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("先执行第一个任务");
            return 1024;
        });
        /**
         * 调用thenApply方法执行第二个任务时，则第二个任务和第一个任务是共用同一个线程池。
         * 调用thenApplyAsync执行第二个任务时，则第一个任务使用的是你自己传入的线程池，第二个任务使用的是ForkJoin线程池
         */
        CompletableFuture<Integer> completableFuture1 = completableFuture.thenApplyAsync(a -> {
            System.out.println("第一个方法执行的内容是 " + a);
            return a + 1;
        });
        //第二个回调方法返回值
        Integer join = completableFuture1.join();
        System.out.println(join);
    }
}

```



##### 5、exceptionally

```JAVA
package com.deng.future_task;

import java.util.concurrent.CompletableFuture;

/**
 * exceptionally 用法
 * 某个任务执行异常时，执行的回调方法;并且有抛出异常作为参数，传递到回调方法
 */
public class CompletableFutureDemo5 {
    public static void main(String[] args) {
        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程名称：" + Thread.currentThread().getName());
            throw new RuntimeException();
//            return 1;
        });
        //如果没抛出异常那么不会走下面
        CompletableFuture<Integer> exceptionally = completableFuture.exceptionally(e -> {
            e.printStackTrace();
            return 1024;
        });
        //当前
        System.out.println(exceptionally.join());
    }
}

```



##### 6、whenComplete

```java
package com.deng.future_task;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * whenComplete 方法
 * 此方法表示，当某个任务执行完成后，执行的回调方法，无返回值，并且whenComplete放回的CompletableFuture的result是上个任务的结果
 */
public class CompletableFutureDemo6 {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程名称：" + Thread.currentThread().getName());
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "第一个方法的返回值";
        }, executorService);
        /**
         * 调用whenComplete方法执行第二个任务时，则第二个任务和第一个任务是共用同一个线程池。
         * 调用whenCompleteAsync执行第二个任务时，则第一个任务使用的是你自己传入的线程池，第二个任务使用的是ForkJoin线程池
         */
        CompletableFuture<String> task2 = task1.whenCompleteAsync((a, throwable) -> {
            System.out.println("当前线程名称：" + Thread.currentThread().getName());
            System.out.println("上个任务的返回值是：" + a);
        });
        //可以看出第二个任务输出的内容是第一个任务的返回值
        System.out.println(task2.join());
        executorService.shutdown();
    }
}

```



##### 7、handle方法

```JAVA
package com.deng.future_task;

import java.util.concurrent.CompletableFuture;

/**
 * handle方法
 * 某个任务执行完成后，执行回调方法，并且是有返回值的;并且handle方法返回的CompletableFuture的result是回调方法执行的结果
 */
public class CompletableFutureDemo7 {
    public static void main(String[] args) {
        CompletableFuture<String> orgFuture = CompletableFuture.supplyAsync(
                () -> {
                    System.out.println("当前线程名称：" + Thread.currentThread().getName());
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return "第一个方法的返回值";
                }
        );

        CompletableFuture<String> rstFuture = orgFuture.handle((a, throwable) -> {

            System.out.println("上个任务执行完啦，还把" + a + "传过来");

            return "第二个方法的返回值";
        });
        //返回的是第二个方法的返回值
        System.out.println(rstFuture.join());

    }
}

```



##### 8、thenCombine / thenAcceptBoth / runAfterBoth

```java
package com.deng.future_task;


import java.util.concurrent.CompletableFuture;

/**
 * thenCombine / thenAcceptBoth / runAfterBoth都表示：
 * 将两个CompletableFuture组合起来，只有这两个都正常执行完了，才会执行某个任务。
 * 区别：  thenCombine：会将两个任务的执行结果作为方法入参，传递到指定方法中，且有返回值
 * thenAcceptBoth: 会将两个任务的执行结果作为方法入参，传递到指定方法中，且无返回值
 * runAfterBoth 不会把执行结果当做方法入参，且没有返回值。
 */
public class CompletableFutureDemo8 {
    public static void main(String[] args) {
        //第一个任务
        CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> 11);

        //第二个任务
        CompletableFuture<Integer> task2 = CompletableFuture.supplyAsync(() -> 22);

        //这是第三个任务去执行
        CompletableFuture<Integer> sum = task2.thenCombineAsync(task1, (s, w) -> {
            System.out.println(s);
            System.out.println(w);
            return s + w;
        });
        //返回的是任务一跟任务二组合值
        System.out.println(sum.join());
    }
}

```



##### 9、applyToEither / acceptEither / runAfterEither

```JAVA
package com.deng.future_task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * applyToEither / acceptEither / runAfterEither 都表示：
 * 将两个CompletableFuture组合起来，只要其中一个执行完了,就会执行某个任务。
 * 区别：
 * applyToEither：会将已经执行完成的任务，作为方法入参，传递到指定方法中，且有返回值
 * acceptEither: 会将已经执行完成的任务，作为方法入参，传递到指定方法中，且无返回值
 * runAfterEither： 不会把执行结果当做方法入参，且没有返回值。
 */
public class CompletableFutureDemo9 {
    public static void main(String[] args) {
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("执行完第一个任务");
            return "第一个任务";
        });
        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("执行完第二个任务");
            return "第二个任务";
        });
        CompletableFuture<Void> completableFuture = task2.acceptEither(task1, System.out::println);

        System.out.println(completableFuture.join());
    }
}

```



##### 10、AllOf

```java
package com.deng.future_task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * AllOf
 * 所有任务都执行完成后，才执行 allOf返回的CompletableFuture。
 * 如果任意一个任务异常，allOf的CompletableFuture，执行get方法，会抛出异常
 */
public class CompletableFutureDemo10 {
    public static void main(String[] args) {
        CompletableFuture<Void> task1 = CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("任务一执行完了");
        });
        CompletableFuture<Void> task2 = CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("任务二执行完了");
        });

        try {
            CompletableFuture.allOf(task1, task2).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }


    }
}

```



##### 11、AnyOf 

```JAVA
package com.deng.future_task;

import java.util.concurrent.CompletableFuture;

/**
 * AnyOf 任意一个任务执行完 就返回 如果执行任务出现异常 那么就会get方法需要抛出
 */
public class CompletableFutureDemo11 {
    public static void main(String[] args) {

        CompletableFuture<Void> a = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("我执行完了");
        });
        CompletableFuture<Void> b = CompletableFuture.runAsync(() -> {
            System.out.println("我也执行完了");
        });
        CompletableFuture<Object> anyOfFuture = CompletableFuture.anyOf(a, b).whenComplete((m, k) -> {
            System.out.println("finish");
//            return "捡田螺的小男孩";
        });
        anyOfFuture.join();


    }
}

```







## 16、JMM

###### **Volatile是什么？**

Volatile是java虚拟机提供的轻量级的同步机制

1. <font color = 'red'>保证可见性</font>
2. <font color = 'red'>不保证原子性</font>
3. <font color = 'red'>禁止指令重排</font>

###### 什么是JMM?

​	JMM:java内存模型，不存在的东西，概念，约定！



###### 关于JMM的一些同步约定：

1、线程解锁前，必须共享变量<font color = 'red'>立刻</font>刷回主内存

2、线程加锁前，必须读取主内存中的最新值到工作内存中！

3、加锁和解锁是同一把锁



主存 ->线程A （工作内存A，执行引擎)

​		 ->线程B-  (工作内存B，执行引擎)



##### 

![image-20230331171740973](F:\github_dengl\juc\image-20230331171740973.png)

#### 八种原子操作：

##### 4组原子操作



**1、lock（锁定）**：作用于主内存中的变量，把一个变量表示为一个线程独占的状态

**2、unlock（解锁）**：作用于主内存中的变量，把一个处于锁定状态的变量释放出来，释放后的变量才可以被其他线程锁定



**3、read（读取）**：作用于主内存的变量，把一个变量的值从主内存读取到线程的工作内存中，以便于后面的load操作

**4、load（载入）**：作用于工作内存中的变量，把read操作从主存中得到的变量值放入工作内存中的变量副本



**5、use（使用）**：作用于工作内存中的变量，把工作内存中的一个变量的值传递给执行引擎，每当虚拟机遇到一个需要使用变量的值的字节码指令时将会执行这个操作

**6、assign（赋值）**：作用于工作内存中的变量，把一个从执行引擎接收到的值赋给工作内存的变量，每当虚拟机遇到一个给变量赋值的字节码指令时就执行这个操作

**7、store（存储）**：作用于工作内存中的变量，把工作内存中一个变量的值传送给主存中以便于后面的write操作

**8、write（写入）**：作用于主内存中的变量，把store操作从工作内存中得到的变量的值放入主内存的变量中



**JMM对这八种指令的使用，制定了如下规则 :**
**1、**不允许read和load、store和write操作之一单独出现。即使用了read必须load，使用了store必须write。不允许线程丢弃他最近的assign操作，即工作变量的数据改变了之后，必须告知主存
**2、**不允许一个线程将没有assign的数据从工作内存同步回主内存
**3、**一个新的变量必须在主内存中诞生，不允许工作内存直接使用一个未被初始化的变量。就是对变量实施use、store操作之前，必须经过assign和load操作
**4、**一个变量同一时间只有一个线程能对其进行lock。多次lock后，必须执行相同次数的unlock才能解锁。如果对一个变量进行lock操作，会清空所有工作内存中此变量的值，在执行引擎使用这个变量前，必须重新load或assign操作初始化变量的值
**5、**如果一个变量没有被lock，就不能对其进行unlock操作。也不能unlock一个被其他线程锁住的变量。 对一个变量进行unlock操作之前，必须把此变量同步回主内存





```JAVA
package com.deng.volatile_test;

import java.util.concurrent.TimeUnit;

/**
 * @author DengLei
 * @date 2023/04/11 16:40
 */

public class JMMDemo {
    //没加 volatile 发现main方法并未停止
    private static int num = 0;

    public static void main(String[] args) {

        new Thread(() -> { //线程1
            while (num == 0) {

            }
        }).start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        num = 1;
        System.out.println(Thread.currentThread().getName() + "" + num);
    }
}

```

执行main方法 发现能够正常打印num值1 但是主线程结束以后 线程1并未结束

是因为 主内存中的num 在线程1中的变化是不知道的。



## 17、Volatile



#### 1、可见性

```
package com.deng.volatile_test;

import java.util.concurrent.TimeUnit;

/**
 * @author DengLei
 * @date 2023/04/11 16:40
 */

public class JMMDemo {
    //没加 volatile 发现main方法并未停止会进入死循环
    
    private volatile static int num = 0;

    public static void main(String[] args) {

        new Thread(() -> { //线程1
            while (num == 0) {

            }
        }).start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        num = 1;
        System.out.println(Thread.currentThread().getName() + "" + num);
    }
}

```



#### 2、不保证原子性

原子性：不可分割 要么都成功 要么都失败

```JAVA
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

        //理论上num结果是2万
        for (int i = 0; i < 20; i++) {
            new Thread(() -> {
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

```



###### 如果不加 lock 或者synchronized ，如何保证原子性？





#### 3、禁止指令重排






















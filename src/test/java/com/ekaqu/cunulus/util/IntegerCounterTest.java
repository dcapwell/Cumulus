package com.ekaqu.cunulus.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Test(groups = "Unit")
public class IntegerCounterTest {

  private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
      .setDaemon(true)
      .setNameFormat(IntegerCounterTest.class.getSimpleName())
      .build();
  private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2 + 1;

  public void testGet() throws Exception {
    IntegerCounter counter = new IntegerCounter(5);
    Assert.assertEquals(counter.get(), 5, "Get didn't return the expected value");
  }

  public void testSet() throws Exception {
    IntegerCounter counter = new IntegerCounter();
    counter.set(5);
    Assert.assertEquals(counter.get(), 5, "Get didn't return the expected value");
  }

  public void testLazySet() throws Exception {
    IntegerCounter counter = new IntegerCounter();
    counter.lazySet(5);
    Assert.assertEquals(counter.get(), 5, "Get didn't return the expected value");
  }

  public void testGetAndSet() throws Exception {
    IntegerCounter counter = new IntegerCounter(5);
    Assert.assertEquals(counter.getAndSet(12), 5, "GetAndSet didn't return the expected value");
    Assert.assertEquals(counter.get(), 12, "Get didn't return the expected value");
  }

  public void testCompareAndSet() throws Exception {
    IntegerCounter counter = new IntegerCounter(5);
    Assert.assertFalse(counter.compareAndSet(0, 12), "Set should have been rejected");
    Assert.assertEquals(counter.get(), 5, "Get didn't return the expected value");
    Assert.assertTrue(counter.compareAndSet(5, 12), "Set should have been accepted");
    Assert.assertEquals(counter.get(), 12, "Get didn't return the expected value");
  }

  public void testWeakCompareAndSet() throws Exception {
    IntegerCounter counter = new IntegerCounter(5);
    Assert.assertFalse(counter.weakCompareAndSet(0, 12), "Set should have been rejected");
    Assert.assertEquals(counter.get(), 5, "Get didn't return the expected value");
    Assert.assertTrue(counter.weakCompareAndSet(5, 12), "Set should have been accepted");
    Assert.assertEquals(counter.get(), 12, "Get didn't return the expected value");
  }

  public void testGetAndIncrement() throws Exception {
    IntegerCounter counter = new IntegerCounter(5);
    Assert.assertEquals(counter.getAndIncrement(), 5, "Get and Increment didn't return expeced value");
    Assert.assertEquals(counter.get(), 6, "Get didn't return the expected value");
  }

  public void testGetAndDecrement() throws Exception {
    IntegerCounter counter = new IntegerCounter(5);
    Assert.assertEquals(counter.getAndDecrement(), 5, "Get and Decrement didn't return expeced value");
    Assert.assertEquals(counter.get(), 4, "Get didn't return the expected value");
  }

  public void testGetAndAdd() throws Exception {
    IntegerCounter counter = new IntegerCounter(5);
    Assert.assertEquals(counter.getAndAdd(5), 5, "Get and Decrement didn't return expeced value");
    Assert.assertEquals(counter.get(), 10, "Get didn't return the expected value");
  }

  public void testIncrementAndGet() throws Exception {
    IntegerCounter counter = new IntegerCounter(5);
    Assert.assertEquals(counter.incrementAndGet(), 6, "Get and Increment didn't return expeced value");
    Assert.assertEquals(counter.get(), 6, "Get didn't return the expected value");
  }

  public void testDecrementAndGet() throws Exception {
    IntegerCounter counter = new IntegerCounter(5);
    Assert.assertEquals(counter.decrementAndGet(), 4, "Get and Decrement didn't return expeced value");
    Assert.assertEquals(counter.get(), 4, "Get didn't return the expected value");
  }

  public void testAddAndGet() throws Exception {
    IntegerCounter counter = new IntegerCounter(5);
    Assert.assertEquals(counter.addAndGet(5), 10, "Get and Decrement didn't return expeced value");
    Assert.assertEquals(counter.get(), 10, "Get didn't return the expected value");
  }

  public void testToString() throws Exception {
    IntegerCounter counter = new IntegerCounter(12);
    Assert.assertEquals(counter.toString(), "12", "toString didn't give expected value");
  }

  public void testIntValue() throws Exception {
    IntegerCounter counter = new IntegerCounter(12);
    Assert.assertEquals(counter.intValue(), 12);
  }

  public void testLongValue() throws Exception {
    IntegerCounter counter = new IntegerCounter(12);
    Assert.assertEquals(counter.longValue(), 12l);
  }

  public void testFloatValue() throws Exception {
    IntegerCounter counter = new IntegerCounter(12);
    Assert.assertEquals(counter.floatValue(), 12f);
  }

  public void testDoubleValue() throws Exception {
    IntegerCounter counter = new IntegerCounter(12);
    Assert.assertEquals(counter.doubleValue(), 12d);
  }

  public void testAddAndGetWithMax() {
    IntegerCounter counter = new IntegerCounter(Integer.MAX_VALUE);
    // 4 because +1 makes it roll over, so 4 remains
    Assert.assertEquals(counter.addAndGet(5), Integer.MIN_VALUE + 4, "Get and Decrement didn't return expeced value");
  }

  public void testAddAndGetWithMin() {
    IntegerCounter counter = new IntegerCounter(Integer.MIN_VALUE);
    // 4 because +1 makes it roll over, so 4 remains
    Assert.assertEquals(counter.addAndGet(-5), Integer.MAX_VALUE - 4, "Get and Decrement didn't return expeced value");
  }

  @Test(groups = "Experiment", description = "Shows that the class isn't thread safe")
  public void concurrentIncrement() throws InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT, THREAD_FACTORY);
    final IntegerCounter counter = new IntegerCounter();
    for(int i = 0; i < 500; i++) {
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          counter.incrementAndGet();
        }
      });
    }
    executorService.shutdown();
    executorService.awaitTermination(20, TimeUnit.SECONDS);

    Assert.assertEquals(counter.get(), 500);
  }

  @Test(groups = "Experiment", description = "Shows that the class isn't thread safe")
  public void concurrentDecrement() throws InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT, THREAD_FACTORY);
    final IntegerCounter counter = new IntegerCounter(500);
    for(int i = 0; i < 500; i++) {
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          counter.decrementAndGet();
        }
      });
    }
    executorService.shutdown();
    executorService.awaitTermination(20, TimeUnit.SECONDS);

    Assert.assertEquals(counter.get(), 0);
  }

  @Test(groups = "Experiment", description = "Shows that the class isn't thread safe")
  public void concurrentEnvironment() throws InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT, THREAD_FACTORY);
    final IntegerCounter counter = new IntegerCounter();
    for(int i = 0; i < 500; i++) {
      final int finalI = i;
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          if(finalI % 2 == 0) {
            counter.incrementAndGet();
          } else {
            counter.decrementAndGet();
          }
        }
      });
    }
    executorService.shutdown();
    executorService.awaitTermination(20, TimeUnit.SECONDS);

    Assert.assertEquals(counter.get(), 0);
  }
}

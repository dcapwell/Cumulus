package com.ekaqu.cunulus.util;

import com.ekaqu.cunulus.ThreadPools;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.cliffc.high_scale_lib.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple test to see the difference in speed between two different counter implementations with and without concurrent operations.
 * <p/>
 * This in no way shows one is faster than the other since the tests are contrived and not real world
 * <p/>
 * Findings:
 * <p/>
 * For single threaded environments it seems that both Atomic classes out perform the Counter.
 * For multi threaded environments it really depends on the amount of parallel operations.  Once parallel operations get
 * large enough the counter class starts to break away from the Atomic classes.  Interesting note is that both Counter
 * and AtomicLong start to pull away from AtomicInt which seems to get slower the more operations happening.
 */
@Test(groups = "Experiment")
public class CounterPerfCompair {
  private static final Logger LOGGER = LoggerFactory.getLogger(CounterPerfCompair.class.getName());

  private final StringBuilder report = new StringBuilder();

  private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(ThreadPools.getMaxSizePool(CounterPerfCompair.class));

  @BeforeClass(alwaysRun = true)
  public void begin() {
    // warm up the JVM a little and the libs
    final AtomicInteger atomicInteger = new AtomicInteger();
    final AtomicLong atomicLong = new AtomicLong();
    final Counter counter = new Counter();
    final IntegerCounter integerCounter = new IntegerCounter();
    final LongCounter longCounter = new LongCounter();

    final List<ListenableFuture<?>> futures = Lists.newArrayList();
    for (int i = 0; i < 100; i++) {
      ListenableFuture<?> future = executorService.submit(new Runnable() {
        @Override
        public void run() {
          atomicInteger.incrementAndGet();
          atomicLong.incrementAndGet();
          counter.increment();
          integerCounter.incrementAndGet();
          longCounter.incrementAndGet();
        }
      });
      futures.add(future);
    }

    for (int i = 0; i < 100; i++) {
      atomicInteger.decrementAndGet();
      atomicLong.decrementAndGet();
      counter.decrement();
      integerCounter.decrementAndGet();
      longCounter.decrementAndGet();
    }

    Futures.successfulAsList(futures);
  }

  @BeforeMethod(alwaysRun = true)
  public void attemptGC() {
    System.gc(); // attempt to GC so GC doesn't get in the way.  This doesn't guaranties it happens
  }

  @AfterClass(alwaysRun = true)
  public void report() {
    LOGGER.info("Report\r\n{}", report);
  }

  /**
   * With multiple iterations and a single thread it seems that the order in speed is as follows:
   * <p/>
   * AtomicInt, AtomicLong, Counter
   * <p/>
   * With one iteration the order in speed is as follows:
   * <p/>
   * AtomicLong, AtomicInteger, Counter
   * <p/>
   * Int++ not added above since its not mutable when marked final which limits usage.  Speed is an order of magnitude faster as expected
   */
  @Test(dataProvider = "iterationCounts")
  public void singleThreaded(final int iterations) {
    final Stopwatch stopwatch = new Stopwatch();

    AtomicInteger atomicInteger = new AtomicInteger();
    AtomicLong atomicLong = new AtomicLong();
    Counter counter = new Counter();
    IntegerCounter integerCounter = new IntegerCounter();
    LongCounter longCounter = new LongCounter();
    int count = 0;

    stopwatch.start();
    for (int i = 0; i < iterations; i++) {
      count++;
    }
    stopwatch.stop();

    System.gc();

    addToReport("Single Thread", "Int++", iterations, stopwatch);

    stopwatch.reset().start();
    for (int i = 0; i < iterations; i++) {
      integerCounter.incrementAndGet();
    }
    stopwatch.stop();

    System.gc();

    addToReport("Single Thread", "Integer Counter Class", iterations, stopwatch);

    stopwatch.reset().start();
    for (int i = 0; i < iterations; i++) {
      longCounter.incrementAndGet();
    }
    stopwatch.stop();

    System.gc();

    addToReport("Single Thread", "Long Counter Class", iterations, stopwatch);

    stopwatch.reset().start();
    for (int i = 0; i < iterations; i++) {
      atomicInteger.incrementAndGet();
    }
    stopwatch.stop();

    System.gc();

    addToReport("Single Thread", "Atomic Integer", iterations, stopwatch);

    stopwatch.reset().start();
    for (int i = 0; i < iterations; i++) {
      atomicLong.incrementAndGet();
    }
    stopwatch.stop();

    System.gc();

    addToReport("Single Thread", "Atomic Long", iterations, stopwatch);

    stopwatch.reset().start();
    for (int i = 0; i < iterations; i++) {
      counter.increment();
    }
    stopwatch.stop();

    addToReport("Single Thread", "Counter", iterations, stopwatch);
  }

  /**
   * With multiple iterations and multiple threads it seems that the order in speed is as follows:
   * <p/>
   * 10000 iterations Counter AtomicLong AtomicInteger
   * 1000 iterations (Counter, AtomicLong), AtomicInt
   * 100 iterations even
   * 10 even
   * 1 AtomicInt (Counter, AtomicLong) // thread overhead is probably the cause
   * <p/>
   * With one iteration the order in speed is as follows:
   * <p/>
   * AtomicLong, AtomicInteger, Counter
   */
  @Test(dataProvider = "iterationCounts")
  public void multiThreaded(final int iterations) throws ExecutionException, InterruptedException {
    final Stopwatch stopwatch = new Stopwatch();

    final AtomicInteger atomicInteger = new AtomicInteger();
    final AtomicLong atomicLong = new AtomicLong();
    final Counter counter = new Counter();

    final List<ListenableFuture<?>> atomicIntegerFutures = Lists.newArrayList();
    stopwatch.start();
    for (int i = 0; i < iterations; i++) {
      final int finalI = i;
      ListenableFuture<?> future = executorService.submit(new Runnable() {
        @Override
        public void run() {
          if (finalI % 2 == 0) {
            atomicInteger.incrementAndGet();
          } else {
            atomicInteger.decrementAndGet();
          }
        }
      });
      atomicIntegerFutures.add(future);
    }
    Futures.successfulAsList(atomicIntegerFutures).get();
    stopwatch.stop();

    System.gc();

    addToReport("Multi Thread", "Atomic Integer", iterations, stopwatch);

    final List<ListenableFuture<?>> atomicLongFutures = Lists.newArrayList();
    stopwatch.reset().start();
    for (int i = 0; i < iterations; i++) {
      final int finalI = i;
      ListenableFuture<?> future = executorService.submit(new Runnable() {
        @Override
        public void run() {
          if (finalI % 2 == 0) {
            atomicLong.incrementAndGet();
          } else {
            atomicLong.decrementAndGet();
          }
        }
      });
      atomicLongFutures.add(future);
    }
    Futures.successfulAsList(atomicLongFutures).get();
    stopwatch.stop();

    System.gc();

    addToReport("Multi Thread", "Atomic Long", iterations, stopwatch);

    final List<ListenableFuture<?>> counterFutures = Lists.newArrayList();
    stopwatch.reset().start();
    for (int i = 0; i < iterations; i++) {
      final int finalI = i;
      ListenableFuture<?> future = executorService.submit(new Runnable() {
        @Override
        public void run() {
          if (finalI % 2 == 0) {
            counter.increment();
          } else {
            counter.decrement();
          }
        }
      });
      counterFutures.add(future);
    }
    Futures.successfulAsList(counterFutures).get();
    stopwatch.stop();

    addToReport("Multi Thread", "Counter", iterations, stopwatch);

    if (iterations > 1) {
      Assert.assertEquals(atomicInteger.get(), 0, "Int not what was expected");
      Assert.assertEquals(atomicLong.get(), 0, "Long not what was expected");
      Assert.assertEquals(counter.get(), 0, "Counter not what was expected");
    } else {
      Assert.assertEquals(atomicInteger.get(), 1, "Int not what was expected");
      Assert.assertEquals(atomicLong.get(), 1, "Long not what was expected");
      Assert.assertEquals(counter.get(), 1, "Counter not what was expected");
    }
  }

  private void addToReport(final String name, final String counterName, final int iterations, final Stopwatch stopwatch) {
    synchronized (report) {
      report.append(name).append("\tIteration Count ").append(iterations)
          .append(",\t").append(counterName)
          .append(" time ").append(stopwatch.elapsedTime(TimeUnit.NANOSECONDS)).append("\r\n");
    }
  }

  @DataProvider
  public static Object[][] iterationCounts() {
    return new Object[][]{
        new Object[]{1},
        new Object[]{10},
        new Object[]{100},
        new Object[]{1000},
        new Object[]{10000},
    };
  }
}

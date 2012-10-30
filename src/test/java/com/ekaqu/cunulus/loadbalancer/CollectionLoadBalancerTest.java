package com.ekaqu.cunulus.loadbalancer;

import com.ekaqu.cunulus.ThreadPools;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Test(groups = "Unit")
public class CollectionLoadBalancerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(CollectionLoadBalancerTest.class.getName());

  private final LoadBalancer<Integer> DELEGATE_BALANCER = new MinLoadBalancer<Integer>(Ordering.<Integer>natural());

  private final int MAX_ITERATIONS_THREADED = 100000;

  public void emptyList() {
    CollectionLoadBalancer<Integer> lb = new CollectionLoadBalancer<Integer>(ImmutableList.<Integer>of(), DELEGATE_BALANCER);
    for(int i = 0; i < 10; i++) {
      final Object result = lb.get();
      Assert.assertNull(result, "Result should be null");
    }
  }

  public void singleElementList() {
    CollectionLoadBalancer<Integer> lb = new CollectionLoadBalancer<Integer>(Arrays.asList(2), DELEGATE_BALANCER);
    for(int i = 0; i < 10; i++) {
      final Object result = lb.get();
      Assert.assertEquals(result, Integer.valueOf(2));
    }
  }

  public void multiElementList() {
    CollectionLoadBalancer<Integer> lb = new CollectionLoadBalancer<Integer>(Arrays.<Integer>asList(12, 24, 82, 12, 51, 298, 2, 6982), DELEGATE_BALANCER);
    final Integer smallest = lb.get();
    Assert.assertEquals(smallest, Integer.valueOf(2), "Not smallest value");
  }

  public void concurrentModifyAdd() throws InterruptedException {
//    final Set<Long> set = new ConcurrentSkipListSet<Long>();
    final Map<Long, Long> map = Maps.newConcurrentMap();
    ExecutorService executorService = ThreadPools.getMaxSizePool(this);
    final CollectionLoadBalancer<Map.Entry<Long, Long>> loadBalancer = new CollectionLoadBalancer<Map.Entry<Long, Long>>(map.entrySet(), new RoundRobinLoadBalancer<Map.Entry<Long, Long>>());

    final int iterations = MAX_ITERATIONS_THREADED;
    final AtomicReference<Throwable> lastThrowable = new AtomicReference<Throwable>();
    for(int i = 0; i < iterations; i++) {
      final int finalI = i;
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          try {
            if(finalI % 2 == 0) {
              final long time = System.nanoTime();
              map.put(time, time);
            } else {
              Map.Entry<Long, Long> number = loadBalancer.get();
              Assert.assertNotNull(number);
            }
          } catch (Throwable ex) {
            lastThrowable.set(ex);
          }
        }
      });
    }

    executorService.shutdown();
    executorService.awaitTermination(50, TimeUnit.MINUTES);

    LOGGER.info("Last Thrown Exception: {}", lastThrowable);
    Assert.assertNull(lastThrowable.get(), "Exception was thrown");
  }

  public void concurrentModifyAddAndDelete() throws InterruptedException {
//    final Set<Long> set = new ConcurrentSkipListSet<Long>();
    final Map<Long, Long> map = Maps.newConcurrentMap();
    ExecutorService executorService = ThreadPools.getMaxSizePool(this);
    final CollectionLoadBalancer<Map.Entry<Long, Long>> loadBalancer = new CollectionLoadBalancer<Map.Entry<Long, Long>>(map.entrySet(), new RoundRobinLoadBalancer<Map.Entry<Long, Long>>());

    final int iterations = MAX_ITERATIONS_THREADED;
    final AtomicReference<Throwable> lastThrowable = new AtomicReference<Throwable>();
    for(int i = 0; i < iterations; i++) {
      final int finalI = i;
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          try {
            if(finalI % 2 == 0) {
              final long time = System.nanoTime();
              map.put(time, time);
            } else {
              map.remove(Iterables.get(map.keySet(), 0)); // remove first element
            }
            Map.Entry<Long, Long> number = loadBalancer.get();
            Assert.assertNotNull(number);
          } catch (Throwable ex) {
            lastThrowable.set(ex);
          }
        }
      });
    }

    executorService.shutdown();
    executorService.awaitTermination(50, TimeUnit.MINUTES);

    LOGGER.info("Last Thrown Exception: {}", lastThrowable);
    Assert.assertNull(lastThrowable.get(), "Exception was thrown");
  }

  @Test(groups = "Experiment")
  public void concurrentModifyAddUnsafeCollection() throws InterruptedException {
//    final Set<Long> set = new ConcurrentSkipListSet<Long>();
    final Map<Long, Long> map = Maps.newHashMap();
    ExecutorService executorService = ThreadPools.getMaxSizePool(this);
    final CollectionLoadBalancer<Map.Entry<Long, Long>> loadBalancer = new CollectionLoadBalancer<Map.Entry<Long, Long>>(map.entrySet(), new RoundRobinLoadBalancer<Map.Entry<Long, Long>>());

    final int iterations = MAX_ITERATIONS_THREADED;
    final AtomicReference<Throwable> lastThrowable = new AtomicReference<Throwable>();
    final Object writeLock = new Object();
    for(int i = 0; i < iterations; i++) {
      if(lastThrowable.get() != null) break;

      final int finalI = i;
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          try {
            if(finalI % 2 == 0) {
              final long time = System.nanoTime();
              synchronized (writeLock) {
                map.put(time, time);
              }
            } else {
              Map.Entry<Long, Long> number = loadBalancer.get();
              Assert.assertNotNull(number);
            }
          } catch (Throwable ex) {
            lastThrowable.set(ex);
          }
        }
      });
    }

    executorService.shutdown();
    executorService.awaitTermination(50, TimeUnit.MINUTES);

    LOGGER.info("Last Thrown Exception: {}", lastThrowable.get());
    Assert.assertNull(lastThrowable.get(), "Exception was thrown");
  }
}

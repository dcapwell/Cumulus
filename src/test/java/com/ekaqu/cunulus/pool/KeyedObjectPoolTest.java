package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.ThreadPools;
import com.ekaqu.cunulus.pool.mocks.StringObjectFactory;
import com.ekaqu.cunulus.retry.BackOffPolicy;
import com.ekaqu.cunulus.retry.FixedBackOffPolicy;
import com.ekaqu.cunulus.retry.RandomBackOffPolicy;
import com.ekaqu.cunulus.retry.Retryers;
import com.ekaqu.cunulus.util.Block;
import com.ekaqu.cunulus.util.Factory;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = "Unit")
public class KeyedObjectPoolTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(KeyedObjectPoolTest.class.getName());

  private final StringObjectFactory stringFactory = new StringObjectFactory();
  private final PoolBuilder<String>.KeyedPoolBuilder<String, String> poolBuilder = new PoolBuilder<String>()
      .corePoolSize(1)
      .maxPoolSize(2)
      .withKeyType(String.class)
      .factory(stringFactory)
      .keySupplier(stringFactory);
//  private final ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).build();
//  private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2 + 1;

  private static final int MAX_ITERATIONS = 1000;

  public void createAndGet() {
    KeyedObjectPool<String, String> pool = (KeyedObjectPool<String, String>) poolBuilder.build();

    final int expectedSize = pool.getCorePoolSize();

    // verify start size is as expected
    Assert.assertEquals(pool.size(), expectedSize, "Size doesn't match");
    Assert.assertFalse(pool.isEmpty(), "Pool is empty?");

    // borrow everything in the pool
    List<Map.Entry<String, String>> borrowed = Lists.newArrayList();
    for (int i = 0; i < expectedSize; i++) {
      // don't return, want to verify this goes empty
      borrowed.add(pool.borrow().get());
    }

    LOGGER.info("Pool {}", pool);
    LOGGER.info("Borrowed {}", borrowed);
    Assert.assertTrue(pool.isEmpty(), "Pool is empty");
    Assert.assertEquals(pool.size(), 0, "Pool size isn't 0");
    Assert.assertEquals(borrowed.size(), expectedSize, "Borrowed count not starting sized count");

    // add back to the pool
    for (Map.Entry<String, String> e : borrowed) {
      pool.returnToPool(e);
    }

    LOGGER.info("Pool {}", pool);
    Assert.assertFalse(pool.isEmpty(), "Pool is empty");
    Assert.assertEquals(pool.size(), expectedSize, "Pool size isn't core^2");
  }

  public void isEmpty() {
    KeyedObjectPool<String, String> pool = (KeyedObjectPool<String, String>) poolBuilder.build();
    Assert.assertFalse(pool.isEmpty());

    for (int i = 0, maxSize = pool.getCorePoolSize(); i < maxSize; i++) {
      Assert.assertFalse(pool.isEmpty());
      pool.borrow(5, TimeUnit.SECONDS).get();
    }
    Assert.assertTrue(pool.isEmpty());
  }

  public void drainToMax() {
    KeyedObjectPool<String, String> pool = (KeyedObjectPool<String, String>) poolBuilder.build();

    final int coreSize = pool.getCorePoolSize();
    final int maxSize = pool.getMaxPoolSize();

    // borrow everything in the pool
    List<Map.Entry<String, String>> borrowed = Lists.newArrayList();
    for (int i = 0; i < maxSize; i++) {
      // don't return, want to verify this goes empty
      if (i < coreSize) {
        borrowed.add(pool.borrow().get());
      } else {
        // this is here for debugging only.  Lets the debugger jump to where a new task gets created
        borrowed.add(pool.borrow(5, TimeUnit.SECONDS).get());
      }
    }

    LOGGER.info("Pool {}", pool);
    LOGGER.info("Borrowed {}", borrowed);
    Assert.assertTrue(pool.isEmpty(), "Pool not empty");
    Assert.assertEquals(pool.size(), 0, "Pool size isn't 0");
    Assert.assertEquals(borrowed.size(), maxSize, "Borrowed count not starting sized count");

    // pool more, all should be absent
    for (int i = 0; i < maxSize; i++) {
      Assert.assertFalse(pool.borrow().isPresent(), "Pool returned data");
    }

    // return data to pool
    for (Map.Entry<String, String> e : borrowed) {
      pool.returnToPool(e);
    }

    Assert.assertEquals(pool.size(), pool.getMaxPoolSize(), "Pool should be at max size right now");
  }

  @Test(groups = "Slow")
  public void concurrentExpandingPool() {
    final KeyedPool<String, String> pool = new PoolBuilder<String>()
        .withKeyType(String.class)
        .factory(stringFactory)
        .keySupplier(stringFactory)
        .build();

    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(pool.size(), 5 * 5, "CorePoolSize not set at startup");

    // causes the interactions to be more random in hopes that threads hit at different times
    final ExecutorService executorService = ThreadPools.getMaxSizePool(this);
    final BackOffPolicy backOffPolicy = new RandomBackOffPolicy(500);
    for (int i = 0; i < MAX_ITERATIONS; i++) {
      LOGGER.info("Iteration {}", i);
      final Map.Entry<String, String> obj = pool.borrow(50, TimeUnit.SECONDS).get();

      final int finalI = i;
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          backOffPolicy.backoff(finalI);
          pool.returnToPool(obj);
        }
      });
    }

//    try {
//      TimeUnit.SECONDS.sleep(2);
//    } catch (InterruptedException e) {
//      Thread.currentThread().interrupt();
//    }
    executorService.shutdown();
    try {
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(pool.size(), pool.getMaxPoolSize(), "MaxPoolSize not expanded to");
  }

  @Test(groups = {"Experiment", "Slow"}, description = "A pool without retries isn't guarantied to give a object back.  " +
      "This test is used for testing timing and isn't a unit test")
  public void concurrentExpandingPoolWithExecution() throws InterruptedException {
    final int maxPoolSize = 2;
    final ExecutingPool<Map.Entry<String, String>> pool = new PoolBuilder<String>()
        .maxPoolSize(maxPoolSize)
        .withKeyType(String.class)
        .factory(stringFactory)
        .keySupplier(stringFactory)
        .buildExecutingPool();

    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(pool.size(), pool.getCorePoolSize(), "CorePoolSize not set at startup");

    // causes the interactions to be more random in hopes that threads hit at different times
    final ExecutorService executorService = ThreadPools.getMaxSizePool(this);
    final BackOffPolicy backOffPolicy = new FixedBackOffPolicy(500, TimeUnit.MILLISECONDS);
    final AtomicInteger callCounter = new AtomicInteger();

    final int iterations = MAX_ITERATIONS;
    for (int i = 0; i < iterations; i++) {
      final int finalI = i;
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          pool.execute(new Block<Map.Entry<String, String>>() {
            @Override
            public void apply(final Map.Entry<String, String> stringStringEntry) {
              LOGGER.info("Iteration {}", finalI);

//              backOffPolicy.backoff(finalI);
              callCounter.incrementAndGet();
            }
          }, 50, TimeUnit.SECONDS);
        }
      });
    }

    executorService.shutdown();
    try {
      executorService.awaitTermination(50, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
//    TimeUnit.SECONDS.sleep(10);

    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(callCounter.get(), iterations, "A block didn't execute");
    Assert.assertEquals(pool.size(), pool.getMaxPoolSize(), "MaxPoolSize not expanded to");
  }

  @Test(groups = "Slow")
  public void concurrentExpandingPoolWithRetryExecution() throws InterruptedException {
    final int maxPoolSize = 2;
    final ExecutingPool<Map.Entry<String, String>> pool = new PoolBuilder<String>()
        .maxPoolSize(maxPoolSize)
        .withKeyType(String.class)
        .factory(stringFactory)
        .keySupplier(stringFactory)
        .buildExecutingPool(
            // 10 should be enough so everyone gets an object
            // this will cause the last few executions to be slow since they have longer sleeps
            Retryers.newExponentialBackoffRetryer(10));

    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(pool.size(), pool.getCorePoolSize(), "CorePoolSize not set at startup");

    // causes the interactions to be more random in hopes that threads hit at different times
    final ExecutorService executorService = ThreadPools.getMaxSizePool(this);
    final BackOffPolicy backOffPolicy = new RandomBackOffPolicy(500);
    final AtomicInteger callCounter = new AtomicInteger();

    final int iterations = MAX_ITERATIONS;
    for (int i = 0; i < iterations; i++) {
      final int finalI = i;
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          pool.execute(new Block<Map.Entry<String, String>>() {
            @Override
            public void apply(final Map.Entry<String, String> stringStringEntry) {
              LOGGER.info("Iteration {}", finalI);

              backOffPolicy.backoff(finalI);
              callCounter.incrementAndGet();
            }
          }, 50, TimeUnit.SECONDS);
        }
      });
    }

    executorService.shutdown();
    try {
      executorService.awaitTermination(50, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(callCounter.get(), iterations, "A block didn't execute");
//    Assert.assertEquals(pool.size(), pool.getMaxPoolSize(), "MaxPoolSize not expanded to"); // as long as the number of iterations matches, who cares if expanded!
  }

  public void closedPoolGetsRemoved() {
    Supplier<String> keySupplier = mock(Supplier.class);
    when(keySupplier.get()).thenReturn("1", "2", "3");
    ObjectFactory killPoolFactory = mock(ObjectFactory.class);
    when(killPoolFactory.validate(any(String.class), any(Throwable.class))).thenReturn(ObjectFactory.State.CLOSE_POOL);
    when(killPoolFactory.get()).thenReturn("1", "2", "3");

    Factory<String, ObjectFactory<String>> mockFactory = mock(Factory.class);
    when(mockFactory.get(any(String.class))).thenReturn(killPoolFactory);

    final KeyedPool<String, String> pool = new PoolBuilder<String>()
        .corePoolSize(3)
        .maxPoolSize(6)
        .withKeyType(String.class)
        .keySupplier(keySupplier)
        .factory(mockFactory).build();

    Assert.assertEquals(pool.size(), 3 * 3);

    pool.returnToPool(new AbstractMap.SimpleEntry<String, String>("1", "close"));

    Assert.assertEquals(pool.size(), 2 * 3);
  }
}

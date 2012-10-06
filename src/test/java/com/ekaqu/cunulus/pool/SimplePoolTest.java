package com.ekaqu.cunulus.pool;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Test(groups = "Unit")
public class SimplePoolTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimplePoolTest.class.getName());

  private final Supplier<String> singleNameManager = new Supplier<String>() {
    @Override
    public String get() {
      return getClass().getName();
    }
  };

  private final Supplier<String> increamentName = new Supplier<String>() {
    private final AtomicInteger counter = new AtomicInteger();
    @Override
    public String get() {
      return Integer.toString(counter.getAndIncrement());
    }
  };

  private final ThreadFactory factory = new ThreadFactoryBuilder()
      .setDaemon(true)
      .build();

  public void simplePoolTest() {
    Pool<String> pool = new SimplePool<String>(singleNameManager, MoreExecutors.sameThreadExecutor(), 1, 10);
    pool.startAndWait();

    Optional<String> result = pool.borrow(5, TimeUnit.SECONDS);
    LOGGER.info("Got back result {}", result);
    Assert.assertEquals(result.get(), SimplePoolTest.class.getName() + "$1");
  }

  public void simplePoolTestTakeAllYourData() {
    Pool<String> pool = new SimplePool<String>(singleNameManager, MoreExecutors.sameThreadExecutor(), 1, 10);
    pool.startAndWait();

    for(int i = 0; i < 10; i++) {
      Optional<String> result = pool.borrow(5, TimeUnit.SECONDS);
      LOGGER.info("Got back result {}", result);
      Assert.assertEquals(result.get(), SimplePoolTest.class.getName() + "$1");
    }

    // get again and fail
    Optional<String> result = pool.borrow(1, TimeUnit.NANOSECONDS);
    LOGGER.info("Should be empty {}", result);
    Assert.assertFalse(result.isPresent());
  }

  public void concurrentOperations() throws ExecutionException, InterruptedException {
    int procCount = Runtime.getRuntime().availableProcessors();
    LOGGER.info("Number of processors on this computer {}", procCount);
    int numThreads = procCount * 2 + 1;

    int maxObjects = 10;
    int initObjects = 1;

    final Pool<String> pool = new SimplePool<String>(increamentName,
        MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numThreads/2, factory)), initObjects, maxObjects);
    pool.start();

    ListeningExecutorService service =
        MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numThreads/2, factory));

    List<ListenableFuture<String>> futures = Lists.newArrayList();
    for(int i = 0; i < 200; i++) {
      ListenableFuture<String> result = service.submit(new Callable<String>() {

        @Override
        public String call() throws Exception {
          Optional<String> result = pool.borrow(500, TimeUnit.SECONDS);
          LOGGER.info("Result {}", result);
          pool.returnToPool(result.get());
          return result.get();
        }
      });
      futures.add(result);
    }

    ListenableFuture<List<String>> results = Futures.successfulAsList(futures);
    List<String> complete = results.get();
    for(String c : complete) {
      LOGGER.info("Got back {}", c);
      int num = Integer.parseInt(c);
      Assert.assertTrue(num < maxObjects, "Num ("+num+") is larger than " + maxObjects);
    }
  }
}

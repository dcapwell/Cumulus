package com.ekaqu.cunulus.retry;

import com.ekaqu.cunulus.ThreadPools;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Test(groups = "Unit")
public class DefaultListeningRetryerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultListeningRetryerTest.class.getName());

  private final ExecutorService service = Executors.newSingleThreadExecutor(ThreadPools.DAEMON_FACTORY);

  public void runCallable() throws ExecutionException, InterruptedException {
    ListeningRetryer retryer = Retryers.newListeningRetryer(5, service);
    final AtomicInteger counter = new AtomicInteger();
    ListenableFuture<Integer> future = retryer.submitWithRetry(new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return counter.incrementAndGet();
      }
    });

    int result = future.get();

    LOGGER.info("Got result back {}", result);
    Assert.assertEquals(counter.get(), 1);
    Assert.assertEquals(result, 1);
  }

  public void runCallableWithExceptionRetries() throws Exception {
    ListeningRetryer retryer = Retryers.newListeningRetryer(5, service);
    final AtomicInteger counter = new AtomicInteger();
    ListenableFuture<Integer> future = retryer.submitWithRetry(new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        if (counter.incrementAndGet() == 5) {
          return counter.get();
        }
        throw new Exception("Not it!");
      }
    });

    int result = future.get();
    LOGGER.info("Got result back {}", result);
    Assert.assertEquals(result, 5);
  }

  public void runCallbackNoRetries() throws Exception {
    ListeningRetryer retryer = Retryers.newListeningRetryer(5, service);
    final AtomicInteger counter = new AtomicInteger();
    ListenableFuture<Integer> future = retryer.submitWithRetry(new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return counter.incrementAndGet();
      }
    });

    int result = future.get();
    LOGGER.info("Got result back {}", result);
    Assert.assertEquals(result, 1);
  }

  public void runRunnableWithRetries() {
    ListeningRetryer retryer = Retryers.newListeningRetryer(5, service);
    final AtomicInteger counter = new AtomicInteger();
    ListenableFuture<?> future = retryer.submitWithRetry(new Runnable() {
      @Override
      public void run() {
        counter.incrementAndGet();
        throw new RuntimeException("Not it!");
      }
    });

    try {
      future.get();
      Assert.fail("Exception should have been thrown");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      Throwable t = e.getCause();
      Assert.assertEquals(t.getClass(), RetryException.class);
    }

    LOGGER.info("Got result back {}", counter.get());
    Assert.assertEquals(counter.get(), 6); // run 0 exists with counter = 1, so run 5 exists with counter = 6
  }

  public void runRunnableWithRetriesWithIgnoredValue() {
    ListeningRetryer retryer = Retryers.newListeningRetryer(5, service);
    final AtomicInteger counter = new AtomicInteger();
    ListenableFuture<?> future = retryer.submitWithRetry(new Runnable() {
      @Override
      public void run() {
        counter.incrementAndGet();
        throw new RuntimeException("Not it!");
      }
    }, "Ignored");

    try {
      future.get();
      Assert.fail("Exception should have been thrown");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      Throwable t = e.getCause();
      Assert.assertEquals(t.getClass(), RetryException.class);
    }

    LOGGER.info("Got result back {}", counter.get());
    Assert.assertEquals(counter.get(), 6); // run 0 exists with counter = 1, so run 5 exists with counter = 6
  }

  public void runRunnable() throws ExecutionException, InterruptedException {
    ListeningRetryer retryer = Retryers.newListeningRetryer(5, service);
    final AtomicInteger counter = new AtomicInteger();
    ListenableFuture<?> future = retryer.submitWithRetry(new Runnable() {
      @Override
      public void run() {
        counter.incrementAndGet();
      }
    });

    future.get();

    LOGGER.info("Got result back {}", counter.get());
    Assert.assertEquals(counter.get(), 1);
  }

  public void runRunnableWithValue() throws ExecutionException, InterruptedException {
    ListeningRetryer retryer = Retryers.newListeningRetryer(5, service);
    final AtomicInteger counter = new AtomicInteger();
    ListenableFuture<String> future = retryer.submitWithRetry(new Runnable() {
      @Override
      public void run() {
        counter.incrementAndGet();
      }
    }, "Result");

    String result = future.get();

    LOGGER.info("Got result back {}", result);
    Assert.assertEquals(counter.get(), 1);
    Assert.assertEquals(result, "Result");
  }
}

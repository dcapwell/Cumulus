package com.ekaqu.cumulus.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

@Test(groups = "Unit")
public class DefaultRetryerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRetryerTest.class.getName());

  public void simpleRetryWithExceptions() throws Exception {
    Retryer retryer = Retryers.newRetryer(5);
    final AtomicInteger counter = new AtomicInteger();
    Integer result = retryer.submitWithRetry(new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        if (counter.incrementAndGet() == 5) {
          return counter.get();
        }
        throw new Exception("Not it!");
      }
    });

    LOGGER.info("Got result back {}", result);
    Assert.assertEquals(result.intValue(), 5);
  }

  public void simpleRetryRunnableWithException() throws Exception {
    Retryer retryer = Retryers.newRetryer(5);
    final AtomicInteger counter = new AtomicInteger();
    try {
      retryer.submitWithRetry(new Runnable() {

        @Override
        public void run() {
          counter.incrementAndGet();
          throw new RuntimeException("Not it!");
        }
      });
    } catch (Exception e) {
      Assert.assertEquals(e.getClass(), RetryException.class);
    }

    LOGGER.info("Got result back {}", counter.get());
    Assert.assertEquals(counter.get(), 6); // run 0 exists with counter = 1, so run 5 exists with counter = 6
  }

  public void simpleRetryRunnableWithValue() throws Exception {
    Retryer retryer = Retryers.newRetryer(5);
    final AtomicInteger counter = new AtomicInteger();
    String result = retryer.submitWithRetry(new Runnable() {

      @Override
      public void run() {
        counter.incrementAndGet();
      }
    }, "Result");

    LOGGER.info("Got result back {}", counter.get());
    LOGGER.info("Result returned {}", result);
    Assert.assertEquals(counter.get(), 1); // run 0 exists with counter = 1, so run 5 exists with counter = 6
    Assert.assertEquals(result, "Result");
  }

  public void simpleRetryRunnableWithValueAndException() throws Exception {
    Retryer retryer = Retryers.newRetryer(5);
    final AtomicInteger counter = new AtomicInteger();
    String result = null;
    try {
      result = retryer.submitWithRetry(new Runnable() {

        @Override
        public void run() {
          counter.incrementAndGet();
          throw new RuntimeException("Not it!");
        }
      }, "Result");
    } catch (Exception e) {
      Assert.assertEquals(e.getClass(), RetryException.class);
    }

    LOGGER.info("Got result back {}", counter.get());
    Assert.assertEquals(counter.get(), 6); // run 0 exists with counter = 1, so run 5 exists with counter = 6
    Assert.assertNull(result, "Exception should have thrown so no result");
  }
}

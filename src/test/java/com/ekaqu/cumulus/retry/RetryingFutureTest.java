package com.ekaqu.cumulus.retry;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = "Unit")
public class RetryingFutureTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(RetryingFutureTest.class.getName());

  private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());

  @Test(timeOut = 3000)
  public void simpleReturn() throws Exception {
    final SettableFuture<String> future = SettableFuture.create();
    Callable<ListenableFuture<String>> callable = mock(Callable.class);
    when(callable.call()).thenReturn(future);

    RetryingFuture<String> retryingFuture = RetryingFuture.create(callable);
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        try {
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
          throw Throwables.propagate(e);
        }
        future.set("value");
      }
    });
    final String value = retryingFuture.get();

    Assert.assertEquals(value, "value");
  }

  @Test(expectedExceptions = Exception.class, expectedExceptionsMessageRegExp = ".*Mock Exception")
  public void failedCallable() throws Exception {
    Callable<ListenableFuture<String>> callable = mock(Callable.class);
    when(callable.call()).thenThrow(new Exception("Mock Exception"));

    RetryingFuture<String> retryingFuture = RetryingFuture.create(callable);
    retryingFuture.get();
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*Max Retries must be at least one.*")
  public void zeroRetry() throws Exception {
    Callable<ListenableFuture<String>> callable = mock(Callable.class);
    when(callable.call()).thenThrow(new Exception("Mock Exception"));

    RetryingFuture<String> retryingFuture = RetryingFuture.create(callable, 0);
    retryingFuture.get();
  }

  @Test(expectedExceptions = ExecutionException.class, expectedExceptionsMessageRegExp = ".*Task was cancelled\\.")
  public void killUnderlineFuture() throws Exception {
    final SettableFuture<String> future = SettableFuture.create();
    Callable<ListenableFuture<String>> callable = mock(Callable.class);
    when(callable.call()).thenReturn(future);

    RetryingFuture<String> retryingFuture = RetryingFuture.create(callable);
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        future.cancel(true);
      }
    });
    retryingFuture.get();
  }

  public void killFuture() throws Exception {
    final SettableFuture<String> future = SettableFuture.create();
    Callable<ListenableFuture<String>> callable = mock(Callable.class);
    when(callable.call()).thenReturn(future);

    final RetryingFuture<String> retryingFuture = RetryingFuture.create(callable);
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        retryingFuture.cancel(true);
      }
    });
    try {
      retryingFuture.get();
      Assert.fail("Should have been canceled");
    } catch (ExecutionException e) {
      Assert.assertEquals(e.getCause().getClass(), CancellationException.class);
    }

    Assert.assertTrue(future.isCancelled());
  }

  public void alwaysRejectValue() throws Exception {
    final SettableFuture<String> future = SettableFuture.create();
    Callable<ListenableFuture<String>> callable = mock(Callable.class);
    when(callable.call()).thenReturn(future);

    final RetryingFuture<String> retryingFuture = RetryingFuture.create(callable, 3, Predicates.<String>alwaysFalse());
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        future.set("should be rejected.");
      }
    });
    try {
      retryingFuture.get();
      Assert.fail("Should have been canceled");
    } catch (ExecutionException e) {
      Assert.assertEquals(e.getCause().getClass(), RetryingFuture.RetryValueRejectedException.class);
    }
  }
}
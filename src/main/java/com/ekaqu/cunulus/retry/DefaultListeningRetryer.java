package com.ekaqu.cunulus.retry;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

@Beta
class DefaultListeningRetryer implements ListeningRetryer {
  private final ListeningExecutorService executorService;
  private final Retryer retryer;

  public DefaultListeningRetryer(final ExecutorService executorService, final Retryer retryer) {
    this(MoreExecutors.listeningDecorator(executorService), retryer);
  }

  public DefaultListeningRetryer(final ListeningExecutorService executorService, final Retryer retryer) {
    this.executorService = Preconditions.checkNotNull(executorService);
    this.retryer = Preconditions.checkNotNull(retryer);
  }

  @Override
  public <T> ListenableFuture<T> submitWithRetry(final Callable<T> retryableTask) {
    Preconditions.checkNotNull(retryableTask);
    return executorService.submit(new Callable<T>() {
      @Override
      public T call() throws Exception {
        return retryer.submitWithRetry(retryableTask);
      }
    });
  }

  @Override
  public ListenableFuture<?> submitWithRetry(final Runnable retryableTask) {
    Preconditions.checkNotNull(retryableTask);
    return executorService.submit(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        retryer.submitWithRetry(retryableTask);
        return null;
      }
    });
  }

  @Override
  public <T> ListenableFuture<T> submitWithRetry(final Runnable retryableTask, final T result) {
    Preconditions.checkNotNull(retryableTask);
    return executorService.submit(new Callable<T>() {
      @Override
      public T call() throws Exception {
        retryer.submitWithRetry(retryableTask);
        return result;
      }
    });
  }
}

package com.ekaqu.cunulus.retry;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

/**
 * Defines a way to retry tasks where the result may be returned some time later in the future
 */
public interface ListeningRetryer {

  /**
   * Retry the given {@code Callable} based off a set of conditions
   */
  <T> ListenableFuture<T> submitWithRetry(Callable<T> retryableTask);

  /**
   * Retry the given {@code Runnable} based off a set of conditions
   *
   * @throws com.ekaqu.cunulus.pool.RetryException when runnable throws Exception.  This will be in the future
   */
  ListenableFuture<?> submitWithRetry(Runnable retryableTask);

  /**
   * Retry the given {@code Runnable} based off a set of conditions
   *
   * @throws com.ekaqu.cunulus.pool.RetryException when runnable throws Exception.  This will be in the future
   */
  <T> ListenableFuture<T> submitWithRetry(Runnable retryableTask, T result);
}

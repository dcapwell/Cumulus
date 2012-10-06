package com.ekaqu.cunulus.retry;

import java.util.concurrent.Callable;

/**
 * Defines a way to retry tasks
 */
public interface Retryer {

  /**
   * Retry the given {@code Callable} based off a set of conditions
   */
  <T> T submitWithRetry(Callable<T> retryableTask) throws Exception;

  /**
   * Retry the given {@code Runnable} based off a set of conditions
   *
   * @throws com.ekaqu.cunulus.pool.RetryException when runnable throws Exception
   */
  void submitWithRetry(Runnable retryableTask);

  /**
   * Retry the given {@code Runnable} based off a set of conditions
   *
   * @throws com.ekaqu.cunulus.pool.RetryException when runnable throws Exception
   */
  <T> T submitWithRetry(Runnable retryableTask, T result);
}

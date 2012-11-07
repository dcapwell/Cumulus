package com.ekaqu.cumulus.retry;

import java.util.concurrent.Callable;

/**
 * Defines a way to retry tasks.
 */
public interface Retryer {
  /*
TODO Should this include a Context as input which will let users request retries? Look at how ExecutingPool gets by
without this, kinda hacky.  maybe use a predicate to validate output?  If predicate.apply(output) == false, retry?
Already using this for exceptions
 */

  /**
   * Retry the given {@code Callable} based off a set of conditions.
   *
   * @param retryableTask task to retry
   * @param <T> type returned by task
   * @return value from task
   * @throws Exception thrown from task
   */
  <T> T submitWithRetry(Callable<T> retryableTask) throws Exception;

  /**
   * Retry the given {@code Runnable} based off a set of conditions.
   *
   * @param retryableTask task to retry
   * @throws RetryException when runnable throws Exception
   */
  void submitWithRetry(Runnable retryableTask) throws RetryException;

  /**
   * Retry the given {@code Runnable} based off a set of conditions.
   *
   * @param retryableTask task to retry
   * @param result returned when task is successful
   * @param <T> type returned by task
   * @return value from task
   * @throws RetryException when runnable throws Exception
   */
  <T> T submitWithRetry(Runnable retryableTask, T result) throws RetryException;

  /**
   * Create a proxy around the given target that will retry method execution.
   *
   * @param target object to wrap around retries
   * @param interfaceType interface to proxy with
   * @param <T> type returned by task
   * @return proxy object around target that does retries
   */
  <T> T newProxy(T target, Class<T> interfaceType);
}

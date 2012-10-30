package com.ekaqu.cunulus.retry;

import java.util.concurrent.Callable;

/**
 * Defines a way to retry tasks
 */
//TODO Should this include a Context as input which will let users request retries? Look at how ExecutingPool gets by without this, kinda hacky.
//TODO maybe use a predicate to validate output?  If predicate.apply(output) == false, retry?  Already using this for exceptions
public interface Retryer {

  /**
   * Retry the given {@code Callable} based off a set of conditions
   */
  <T> T submitWithRetry(Callable<T> retryableTask) throws Exception;

  /**
   * Retry the given {@code Runnable} based off a set of conditions
   *
   * @throws RetryException when runnable throws Exception
   */
  void submitWithRetry(Runnable retryableTask);

  /**
   * Retry the given {@code Runnable} based off a set of conditions
   *
   * @throws RetryException when runnable throws Exception
   */
  <T> T submitWithRetry(Runnable retryableTask, T result);

  /**
   * Create a proxy around the given target that will retry method execution
   *
   * @param interfaceType interface to proxy with
   */
  <T> T newProxy(T target, Class<T> interfaceType);
}

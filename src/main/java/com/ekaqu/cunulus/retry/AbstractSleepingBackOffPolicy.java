package com.ekaqu.cunulus.retry;

import com.google.common.annotations.Beta;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Abstract class for all sleep based BackOffPolicys.
 */
@Beta
public abstract class AbstractSleepingBackOffPolicy implements BackOffPolicy {

  @Override
  public void backoff(final int attemptCount) {
    try {
      Thread.sleep(sleepTime(attemptCount));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Determines how long to sleep for in milliseconds.
   * @param attemptCount how many times a retry has happened
   * @return time in milliseconds to sleep for
   */
  abstract long sleepTime(int attemptCount);
}

package com.ekaqu.cumulus.retry;

import com.google.common.annotations.Beta;

/**
 * Abstract class for all sleep based BackOffPolicys.
 */
@Beta
public abstract class AbstractSleepingBackOffPolicy implements BackOffPolicy {

  @Override
  public final void backoff(final int attemptCount) {
    try {
      final long sleepTime = sleepTime(attemptCount);
      Thread.sleep(sleepTime);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Determines how long to sleep for in milliseconds.
   *
   * @param attemptCount how many times a retry has happened
   * @return time in milliseconds to sleep for
   */
  abstract long sleepTime(int attemptCount);
}

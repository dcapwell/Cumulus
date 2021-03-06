package com.ekaqu.cumulus.retry;

/**
 * Defines how to backoff while doing retries.
 */
public interface BackOffPolicy {

  /**
   * Wait for a given amount of time.  This time may change depending on the attemptCount
   * <p/>
   * This method should be thread safe
   *
   * @param attemptCount how many attempts have been seen
   */
  void backoff(int attemptCount);
}

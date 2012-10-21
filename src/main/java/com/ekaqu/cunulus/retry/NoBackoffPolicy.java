package com.ekaqu.cunulus.retry;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A BackOffPolicy that does nothing
 */
@ThreadSafe
public class NoBackoffPolicy implements BackOffPolicy {


  @Override
  public void backoff(final int attemptCount) {
    // do nothing
  }

  private static final BackOffPolicy INSTANCE = new NoBackoffPolicy();

  /**
   * Get a static instance of this class
   */
  public static BackOffPolicy getInstance() {
    return INSTANCE;
  }
}

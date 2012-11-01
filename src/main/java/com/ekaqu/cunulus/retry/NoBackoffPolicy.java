package com.ekaqu.cunulus.retry;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A BackOffPolicy that does nothing.
 */
@ThreadSafe
public final class NoBackoffPolicy implements BackOffPolicy {

  /**
   * Hides constructor from clients.
   */
  private NoBackoffPolicy() {
    // do nothing
  }

  @Override
  public void backoff(final int attemptCount) {
    // do nothing
  }

  /**
   * Static instance to avoid multiple creations.
   */
  private static final BackOffPolicy INSTANCE = new NoBackoffPolicy();

  /**
   * Get a static instance of this class.
   *
   * @return static backoff policy
   */
  public static BackOffPolicy getInstance() {
    return INSTANCE;
  }
}

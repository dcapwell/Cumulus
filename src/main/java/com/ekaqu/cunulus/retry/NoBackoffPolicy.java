package com.ekaqu.cunulus.retry;

public class NoBackoffPolicy implements BackOffPolicy {
  @Override
  public void backoff(final int attemptCount) {
    // do nothing
  }

  private static final BackOffPolicy INSTANCE = new NoBackoffPolicy();

  public static BackOffPolicy getInstance() {
    return INSTANCE;
  }
}

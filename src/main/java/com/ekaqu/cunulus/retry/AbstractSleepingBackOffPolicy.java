package com.ekaqu.cunulus.retry;

public abstract class AbstractSleepingBackOffPolicy implements BackOffPolicy {

  @Override
  public void backoff(final int attemptCount) {
    try {
      Thread.sleep(sleepTime(attemptCount));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  abstract long sleepTime(int attemptCount);
}

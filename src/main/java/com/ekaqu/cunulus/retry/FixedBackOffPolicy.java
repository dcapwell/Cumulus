package com.ekaqu.cunulus.retry;

import java.util.concurrent.TimeUnit;

public class FixedBackOffPolicy extends AbstractSleepingBackOffPolicy {
  private final long sleepTime;

  public FixedBackOffPolicy(final long waitTime, final TimeUnit unit) {
    sleepTime = unit.toMillis(waitTime);
  }

  @Override
  long sleepTime(final int attemptCount) {
    return sleepTime;
  }
}

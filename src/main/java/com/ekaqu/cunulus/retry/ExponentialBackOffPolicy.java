package com.ekaqu.cunulus.retry;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ExponentialBackOffPolicy extends AbstractSleepingBackOffPolicy {
  private final Random random = new Random();
  private final long baseSleepTimeMs;

  public ExponentialBackOffPolicy() {
    this(500, TimeUnit.MILLISECONDS);
  }

  public ExponentialBackOffPolicy(final long baseSleepTime, final TimeUnit unit) {
    this.baseSleepTimeMs = unit.toMillis(baseSleepTime);
  }

  @Override
  long sleepTime(final int attemptCount) {
    return baseSleepTimeMs * Math.max(1, random.nextInt(1 << (attemptCount + 1)));
  }
}

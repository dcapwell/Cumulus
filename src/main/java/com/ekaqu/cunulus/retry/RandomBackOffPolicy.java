package com.ekaqu.cunulus.retry;

import com.google.common.base.Preconditions;

import java.util.Random;

public class RandomBackOffPolicy extends AbstractSleepingBackOffPolicy {

  private final Random random = new Random();
  private final int maxTime;

  public RandomBackOffPolicy(final int maxTime) {
    Preconditions.checkArgument(maxTime > 0, "Max Time must be a positive number");
    this.maxTime = maxTime;
  }

  @Override
  long sleepTime(final int attemptCount) {
    return random.nextInt(maxTime);
  }
}

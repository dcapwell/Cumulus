package com.ekaqu.cunulus.retry;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.TimeUnit;

/**
 * A BackOffPolicy that will sleep for a fixed rate
 */
@ThreadSafe
public class FixedBackOffPolicy extends AbstractSleepingBackOffPolicy {
  private final long sleepTime;

  /**
   * Creates a new BackOffPolicy that sleeps for the given time.
   *
   * @param waitTime how long to sleep
   * @param unit     describes what the long sleep time is
   */
  public FixedBackOffPolicy(final long waitTime, final TimeUnit unit) {
    sleepTime = unit.toMillis(waitTime);
  }

  @Override
  long sleepTime(final int attemptCount) {
    return sleepTime;
  }
}

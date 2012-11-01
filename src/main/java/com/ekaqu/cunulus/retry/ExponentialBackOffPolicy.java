package com.ekaqu.cunulus.retry;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * A BackOffPolicy that will sleep for an exponentially increasing period of time.
 * <p/>
 * The exponential algorithm is the same that was found in <a href="https://github.com/Netflix/astyanax">astyanax</a>.
 */
@ThreadSafe
public final class ExponentialBackOffPolicy extends AbstractSleepingBackOffPolicy {

  /**
   * Default amount of time to wait between retries.
   */
  private static final long DEFAULT_BASE_WAIT_TIME = 500;

  /**
   * Default unit for base wait time.
   */
  private static final TimeUnit DEFAULT_WAIT_UNIT = TimeUnit.MILLISECONDS;

  /**
   * Random used to vary sleep time.
   */
  private final Random random = new Random();

  /**
   * Min amount of time in mills to sleep.
   */
  private final long baseSleepTimeMs;

  /**
   * Creates a new BackOffPolicy with default sleeping time of 500 Milliseconds.
   */
  public ExponentialBackOffPolicy() {
    this(DEFAULT_BASE_WAIT_TIME, DEFAULT_WAIT_UNIT);
  }

  /**
   * Creates a new BackOffPolicy with a base sleep time.
   *
   * @param baseSleepTime how long to sleep
   * @param unit          describes what the long sleep time is
   */
  public ExponentialBackOffPolicy(final long baseSleepTime, final TimeUnit unit) {
    this.baseSleepTimeMs = unit.toMillis(baseSleepTime);
  }

  @Override
  long sleepTime(final int attemptCount) {
    return baseSleepTimeMs * Math.max(1, random.nextInt(1 << (attemptCount + 1)));
  }
}

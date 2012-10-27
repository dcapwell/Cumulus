package com.ekaqu.cunulus.retry;

import java.util.concurrent.ExecutorService;

/**
 * Static utility class for working with {@link Retryer}
 */
public final class Retryers {

  private Retryers() {
  }

  /**
   * Creates a new retryer without a backoff policy
   *
   * @param maxRetries number of retries to allow
   * @return new retryer
   */
  public static final Retryer newRetryer(final int maxRetries) {
    return new DefaultRetryer(maxRetries, NoBackoffPolicy.getInstance());
  }

  /**
   * Creates a new retyer
   *
   * @param maxRetries number of retries to allow
   * @param policy how to backoff between retries
   * @return new retryer
   */
  public static final Retryer newRetryer(final int maxRetries, final BackOffPolicy policy) {
    return new DefaultRetryer(maxRetries, policy);
  }

  /**
   * Creates a new retryer which uses an exponential backoff policy
   *
   * @param maxRetries number of retries to allow
   * @return new retyer
   */
  public static final Retryer newExponentialBackoffRetryer(final int maxRetries) {
    return new DefaultRetryer(maxRetries, new ExponentialBackOffPolicy());
  }

  /**
   * Creates a new listening retryer using the provided executor service
   *
   * @param maxRetries number of retries to allow
   * @param executorService used to run retryer in the background
   * @return new retyer
   */
  public static final ListeningRetryer newListeningRetryer(final int maxRetries, final ExecutorService executorService) {
    Retryer retryer = newRetryer(maxRetries);
    return new DefaultListeningRetryer(executorService, retryer);
  }

  /**
   * Creates a new listening retryer using the provided executor service and backoff plicy
   *
   * @param maxRetries number of retries to allow
   * @param policy how to backoff between retries
   * @param executorService used to run retryer in the background
   * @return new retyer
   */
  public static final ListeningRetryer newListeningRetryer(final int maxRetries, final BackOffPolicy policy, final ExecutorService executorService) {
    Retryer retryer = newRetryer(maxRetries, policy);
    return new DefaultListeningRetryer(executorService, retryer);
  }

  /**
   * Creates a new listening retryer using the provided executor service
   *
   * @param maxRetries number of retries to allow
   * @param executorService used to run retryer in the background
   * @return new retyer
   */
  public static final ListeningRetryer newExponentialBackoffListeningRetryer(final int maxRetries, final ExecutorService executorService) {
    Retryer retryer = newExponentialBackoffRetryer(maxRetries);
    return new DefaultListeningRetryer(executorService, retryer);
  }
}

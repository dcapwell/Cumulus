package com.ekaqu.cunulus.retry;

import java.util.concurrent.ExecutorService;

public final class Retryers {

  private Retryers() {
  }

  public static final Retryer newRetryer(final int maxRetries) {
    return new DefaultRetryer(maxRetries, NoBackoffPolicy.getInstance());
  }

  public static final Retryer newRetryer(final int maxRetries, final BackOffPolicy policy) {
    return new DefaultRetryer(maxRetries, policy);
  }

  public static final Retryer newExponentialBackoffRetryer(final int maxRetries) {
    return new DefaultRetryer(maxRetries, new ExponentialBackOffPolicy());
  }

  public static final ListeningRetryer newListeningRetryer(final int maxRetries, final ExecutorService executorService) {
    Retryer retryer = newRetryer(maxRetries);
    return new DefaultListeningRetryer(executorService, retryer);
  }

  public static final ListeningRetryer newListeningRetryer(final int maxRetries, final BackOffPolicy policy, final ExecutorService executorService) {
    Retryer retryer = newRetryer(maxRetries, policy);
    return new DefaultListeningRetryer(executorService, retryer);
  }

  public static final ListeningRetryer newExponentialBackoffListeningRetryer(final int maxRetries, final ExecutorService executorService) {
    Retryer retryer = newExponentialBackoffRetryer(maxRetries);
    return new DefaultListeningRetryer(executorService, retryer);
  }
}

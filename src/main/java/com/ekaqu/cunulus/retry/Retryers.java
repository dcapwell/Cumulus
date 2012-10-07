package com.ekaqu.cunulus.retry;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;

public class Retryers {

  private Retryers() {}

  public static Retryer newRetryer(final int maxRetries) {
    return new DefaultRetryer(maxRetries, NoBackoffPolicy.getInstance());
  }

  public static Retryer newRetryer(final int maxRetries, final BackOffPolicy policy) {
    return new DefaultRetryer(maxRetries, policy);
  }

  public static Retryer newExponentialBackoffRetryer(final int maxRetries) {
    return new DefaultRetryer(maxRetries, new ExponentialBackOffPolicy());
  }

  public static ListeningRetryer newListeningRetryer(final int maxRetries, final ExecutorService executorService) {
    Retryer retryer = newRetryer(maxRetries);
    return new DefaultListeningRetryer(executorService, retryer);
  }

  public static ListeningRetryer newListeningRetryer(final int maxRetries, final BackOffPolicy policy, final ExecutorService executorService) {
    Retryer retryer = newRetryer(maxRetries, policy);
    return new DefaultListeningRetryer(executorService, retryer);
  }

  public static ListeningRetryer newExponentialBackoffListeningRetryer(final int maxRetries, final ExecutorService executorService) {
    Retryer retryer = newExponentialBackoffRetryer(maxRetries);
    return new DefaultListeningRetryer(executorService, retryer);
  }
}

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

  public static <T> T newProxy(final Retryer retryer, final T target, Class<T> interfaceType) {
    checkNotNull(target, "target");
    checkNotNull(interfaceType, "interfaceType");
    checkNotNull(retryer, "retryer");
    InvocationHandler handler = new InvocationHandler() {
      public Object invoke(final Object obj, final Method method, final Object[] args) throws Throwable {
        Callable<Object> retryableTask = new Callable<Object>() {
          public Object call() throws Exception {
            try {
              method.setAccessible(true); // some methods might not allow us to call, so force accessible
              return method.invoke(target, args);
            } catch (InvocationTargetException e) {
              Throwables.propagateIfPossible(e.getCause(), Exception.class);
              throw new AssertionError("can't get here");
            }
          }
        };
        return retryer.submitWithRetry(retryableTask);
      }
    };

    Object object = Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[]{interfaceType}, handler);
    return interfaceType.cast(object);
  }

  public static <T> T newProxy(final int maxRetries, final T target, Class<T> interfaceType) {
    Retryer retryer = newRetryer(maxRetries);
    return newProxy(retryer, target, interfaceType);
  }

  public static <T> T newExponentialBackoffProxy(final int maxRetries, final T target, Class<T> interfaceType) {
    Retryer retryer = newExponentialBackoffRetryer(maxRetries);
    return newProxy(retryer, target, interfaceType);
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

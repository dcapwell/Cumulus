package com.ekaqu.cunulus.retry;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of the {@link Retryer} interface.  This uses an optional Predicate to allow stopping early for
 * given {@link Exception}s.
 * <p/>
 * This class uses recursion to do retries so large amount of retries might cause stack issues.
 */
@Beta
@ThreadSafe
class DefaultRetryer implements Retryer {
  private final int maxRetries;
  private final BackOffPolicy policy;
  private final Predicate<Exception> exceptionPredicate;

  /**
   * Creates a new Retryer that uses recursion to do retries.  This constructor will accept all exceptions
   *
   * @param maxRetries how may times to retry operations.  Worst case execution count will be maxRetries + 1
   * @param policy     determines how long to wait between retries
   */
  public DefaultRetryer(final int maxRetries, final BackOffPolicy policy) {
    this(maxRetries, policy, Predicates.<Exception>alwaysTrue());
  }

  /**
   * Creates a new Retryer that uses recursion to do retries.
   *
   * @param maxRetries         how many times to retry operations.  Worst case execution count will be maxRetries + 1
   * @param policy             determines how long to wait between retries
   * @param exceptionPredicate allows to break early for given exceptions
   */
  public DefaultRetryer(final int maxRetries, final BackOffPolicy policy,
                        final Predicate<Exception> exceptionPredicate) {
    this.maxRetries = maxRetries;
    this.policy = Preconditions.checkNotNull(policy);
    this.exceptionPredicate = Objects.firstNonNull(exceptionPredicate, Predicates.<Exception>alwaysTrue());
  }

  @Override
  public <T> T submitWithRetry(final Callable<T> retryableTask) throws Exception {
    Preconditions.checkNotNull(retryableTask);
    return submitWithRetry(retryableTask, 0);
  }

  @Override
  public void submitWithRetry(final Runnable retryableTask) {
    Preconditions.checkNotNull(retryableTask);
    try {
      submitWithRetry(Executors.callable(retryableTask), 0);
    } catch (Exception e) {
      throw new RetryException("Runnable threw exception", e);
    }
  }

  @Override
  public <T> T submitWithRetry(final Runnable retryableTask, final T result) {
    Preconditions.checkNotNull(retryableTask);
    try {
      return submitWithRetry(Executors.callable(retryableTask, result));
    } catch (Exception e) {
      throw new RetryException("Runnable threw exception", e);
    }
  }

  @Override
  public <T> T newProxy(final T target, Class<T> interfaceType) {
    checkNotNull(target, "target");
    checkNotNull(interfaceType, "interfaceType");
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
        return submitWithRetry(retryableTask);
      }
    };

    Object object = Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[]{interfaceType}, handler);
    return interfaceType.cast(object);
  }

  /**
   * Retries executing a callable object.  Retries are done using recursion.
   *
   * @throws Exception thrown by the last call to retryableTask
   */
  private <T> T submitWithRetry(final Callable<T> retryableTask, final int retryCount) throws Exception {
    try {
      return retryableTask.call();
    } catch (Exception exception) {
      if (retryCount < maxRetries) {
        if (exceptionPredicate.apply(exception)) {
          policy.backoff(retryCount);
          return submitWithRetry(retryableTask, retryCount + 1);
        }
      }
      throw exception;
    }
  }
}

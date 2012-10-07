package com.ekaqu.cunulus.retry;

import com.ekaqu.cunulus.pool.RetryException;
import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

@Beta
class DefaultRetryer implements Retryer {
  private final int maxRetries;
  private final BackOffPolicy policy;
  private final Predicate<Exception> exceptionPredicate;

  public DefaultRetryer(final int maxRetries, final BackOffPolicy policy) {
    this(maxRetries, policy, Predicates.<Exception>alwaysTrue());
  }

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

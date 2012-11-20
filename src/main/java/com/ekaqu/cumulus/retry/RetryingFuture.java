package com.ekaqu.cumulus.retry;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link java.util.concurrent.Future future} that will retry a task several times until a condition has been meet.
 * For the user, they see a normal {@link ListenableFuture} but the value could be one of several attempts to execute
 * the task where {@link #get()} refers to the final result of the successful task execution.  The max number of
 * executions is {@code 1 + n} where n is the number of retry operations requested.
 *
 * @param <V> value type
 */
public final class RetryingFuture<V> extends AbstractFuture<V> {

  /**
   * The reference to the last thrown exception while trying to call the given task.
   */
  private final AtomicReference<Throwable> lastException = new AtomicReference<Throwable>();

  /**
   * The reference to the last known future returned from the task.
   */
  private final AtomicReference<ListenableFuture<V>> lastFuture = new AtomicReference<ListenableFuture<V>>();

  /**
   * Predicate to determine if the given value is usable or not.
   */
  private final Predicate<V> valuePredicate;

  /**
   * Creates a new Retrying Future.
   *
   * @param callable       task to execute
   * @param maxRetries     to retry. 0 for no retries
   * @param valuePredicate to filter out bad values
   */
  private RetryingFuture(final Callable<ListenableFuture<V>> callable, final int maxRetries,
                         final Predicate<V> valuePredicate) {
    Preconditions.checkArgument(maxRetries >= 0, "Max Retries must be a positive number or zero.");
    this.valuePredicate = Preconditions.checkNotNull(valuePredicate);
    call(callable, maxRetries);
  }

  /**
   * Creates a new Retrying Future.
   *
   * @param callable       task to execute
   * @param maxRetries     to retry
   * @param valuePredicate to filter out bad values
   * @param <V>            value type
   * @return new RetryingFuture
   */
  public static <V> RetryingFuture<V> create(final Callable<ListenableFuture<V>> callable, final int maxRetries,
                                             final Predicate<V> valuePredicate) {
    return new RetryingFuture<V>(callable, maxRetries, valuePredicate);
  }

  /**
   * Creates a new Retrying Future.
   *
   * @param callable   task to execute
   * @param maxRetries to retry
   * @param <V>        value type
   * @return new RetryingFuture
   */
  public static <V> RetryingFuture<V> create(final Callable<ListenableFuture<V>> callable, final int maxRetries) {
    return new RetryingFuture<V>(callable, maxRetries, Predicates.<V>alwaysTrue());
  }

  /**
   * Creates a new Retrying Future.
   *
   * @param callable task to execute
   * @param <V>      value type
   * @return new RetryingFuture
   */
  public static <V> RetryingFuture<V> create(final Callable<ListenableFuture<V>> callable) {
    return new RetryingFuture<V>(callable, 3, Predicates.<V>alwaysTrue());
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    // stop the last future
    final ListenableFuture<V> future = lastFuture.get();
    if (future != null) {
      future.cancel(true);
    }
    return super.cancel(mayInterruptIfRunning);
  }

  /**
   * Calls the callable and adds a callback to retry or return a result.  This method uses recursion to implement
   * retries.
   *
   * @param callable   task to run
   * @param maxRetries to retry
   */
  private void call(final Callable<ListenableFuture<V>> callable, final int maxRetries) {
    Preconditions.checkState(!isDone(), "Future is done, unable to call task");
    if (maxRetries < 0) {
      // hit max retries, fail the future
      setException(lastException.get());
      return;
    }
    try {
      final ListenableFuture<V> future = callable.call();
      lastFuture.set(future);
      Futures.addCallback(future, new FutureCallback<V>() {
        @Override
        public void onSuccess(final V result) {
          if (valuePredicate.apply(result)) {
            set(result);
          } else if (maxRetries == 0) {
            set(result);
          } else {
            onFailure(new RetryValueRejectedException());
          }
        }

        @Override
        public void onFailure(final Throwable t) {
          lastException.set(t);
          call(callable, maxRetries - 1);
        }
      });
    } catch (Exception e) {
      lastException.set(e);
      call(callable, maxRetries - 1);
    }
  }

  /**
   * Exception defining that a value has been rejected.
   */
  private static final class RetryValueRejectedException extends Exception {

    /**
     * Creates a new retry value rejected exception.
     */
    public RetryValueRejectedException() {
      super("Task was unsuccessful; returned value was marked as invalid.");
    }
  }
}

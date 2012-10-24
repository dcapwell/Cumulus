package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.util.Sized;
import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Service;

import java.util.concurrent.TimeUnit;

/**
 * Represents a pool of objects
 *
 * @param <T> type of the object in the pool
 */
@Beta
public interface Pool<T> extends Service, Sized {

  /**
   * This is a non-blocking operation that returns a element from the pool.  If no objects are in the pool
   * then the returned value will be {@code Optional#absent()}
   *
   * @throws ClosedPoolException pool is closed
   */
  Optional<T> borrow();

  /**
   * This is a blocking operation that returns a element from the pool.  A timeout is given to know how long this
   * method is allowed to block for.  If the time has exceeded then the returned value will be empty.
   *
   * @throws ClosedPoolException pool is closed
   */
  Optional<T> borrow(long timeout, TimeUnit unit);

  /**
   * Returns an object to the pool
   *
   * @throws ClosedPoolException pool is closed
   */
  void returnToPool(T obj);

  /**
   * Returns an object to the pool with the last exception thrown.
   *
   * @throws ClosedPoolException pool is closed
   */
  void returnToPool(T obj, Throwable throwable);

  /**
   * The number of elements that this pool wishes to be around
   */
  int getCorePoolSize();

  /**
   * The max number of elements this pool can hold
   */
  int getMaxPoolSize();

  /**
   * The number of objects that currently belong to the pool.
   * <p/>
   * This is different from size because size describes how many elements are currently in the pool.  Active, on the
   * other hand, describes how many elements are alive but not necessarily in the pool at this moment.
   */
  int getActivePoolSize();
}

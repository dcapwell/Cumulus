package com.ekaqu.cunulus.pool;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Service;

import java.util.concurrent.TimeUnit;

/**
 * Represents a pool of objects
 *
 * @param <T> type of the object in the pool
 */
public interface Pool<T> extends Service {

  /**
   * This is a blocking operation that returns a element from the pool.  A timeout is given to know how long this
   * method is allowed to block for.  If the time has exceeded then the returned value will be empty.
   */
  Optional<T> borrow(long timeout, TimeUnit unit);

  /**
   * Returns an object to the pool
   */
  void returnToPool(T obj);

  /**
   * How many elements are currently in the pool
   */
  int size();

  /**
   * Check whether the pool is empty or not
   */
  boolean isEmpty();
}

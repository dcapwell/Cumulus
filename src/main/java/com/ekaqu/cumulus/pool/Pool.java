package com.ekaqu.cumulus.pool;

import com.ekaqu.cumulus.util.Sized;
import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Service;

import java.util.concurrent.TimeUnit;

/**
 * Represents a pool of objects.  A pool is normally used in concurrent environments so most implementations should be
 * thread safe; this interface does not force thread safety though.
 * <p/>
 * It is recommended that while working with pools that the {@link ExecutingPool} is used with a {@link
 * com.ekaqu.cumulus.retry.Retryer}.  This combination will handle most error cases.  Also it is best to use the {@link
 * PoolBuilder} for creating pools.  The builder always returns a thread safe pool and with a few defaults to simplify
 * pool creation.
 * <p/>
 * Example building a Pool using the builder
 * <pre>
 * {@code
 * ExecutingPool<T> pool = new PoolBuilder<T>()
 *                    .objectFactory(poolValueFactory) // defines how to create new pooled entities
 *                    .executorService(executorService) // optional executor service for background pool operations
 *                    .corePoolSize(2) // optional size the pool should try to stay around
 *                    .maxPoolSize(4) // optional max size the pool can reach
 *                    .buildExecutingPool(Retryers.newExponentialBackoffRetryer(10));
 * }
 * </pre>
 * <p/>
 * A simpler example
 * <pre>
 * {@code
 * ExecutingPool<T> pool = new PoolBuilder<T>()
 *                    .objectFactory(poolValueFactory) // defines how to create new pooled entities
 *                    .buildExecutingPool(Retryers.newExponentialBackoffRetryer(10));
 * }
 * </pre>
 *
 * @param <T> type of the object in the pool
 */
@Beta
public interface Pool<T> extends Service, Sized {

  /**
   * This is a non-blocking operation that returns a element from the pool.  If no objects are in the pool then the
   * returned value will be {@link com.google.common.base.Optional#absent()} )}
   *
   * @return optional value from the pool
   * @throws ClosedPoolException pool is closed
   */
  Optional<T> borrow() throws ClosedPoolException;

  /**
   * This is a blocking operation that returns a element from the pool.  A timeout is given to know how long this method
   * is allowed to block for.  If the time has exceeded or the pool is empty then the returned value will be {@link
   * com.google.common.base.Optional#absent()}.
   *
   * @param timeout how long to wait for a new object if pool is empty
   * @param unit timeout unit
   * @return optional value from the pool
   * @throws ClosedPoolException pool is closed
   */
  Optional<T> borrow(long timeout, TimeUnit unit) throws ClosedPoolException;

  /**
   * Returns an object to the pool.  This method might not effect {@link com.ekaqu.cumulus.pool.Pool#size()} since a
   * pool may reject the object presented.
   *
   * @param obj to return to pool
   * @throws ClosedPoolException pool is closed
   */
  void returnToPool(T obj) throws ClosedPoolException;

  /**
   * Returns an object to the pool with the last exception thrown. This method might not effect {@link
   * com.ekaqu.cumulus.pool.Pool#size()} since a pool may reject the object presented.
   *
   * @param obj to return to pool
   * @param throwable thrown when last used the object
   * @throws ClosedPoolException pool is closed
   */
  void returnToPool(T obj, Throwable throwable) throws ClosedPoolException;

  /**
   * The number of elements that this pool wishes to be around.
   *
   * @return min size of the pool
   */
  int getCorePoolSize();

  /**
   * The max number of elements this pool can hold.
   *
   * @return max size of the pool
   */
  int getMaxPoolSize();

  /**
   * The number of objects that currently belong to the pool.
   * <p/>
   * This is different from size because size describes how many elements are currently in the pool.  Active, on the
   * other hand, describes how many elements are alive but not necessarily in the pool at this moment.
   *
   * @return how many elements belong to the pool
   */
  int getActivePoolSize();
}

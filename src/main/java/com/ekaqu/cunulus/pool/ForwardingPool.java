package com.ekaqu.cunulus.pool;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ForwardingService;
import com.google.common.util.concurrent.Service;

import java.util.concurrent.TimeUnit;

/**
 * Forwards all requests to a given pool.  Use this to aid in decorating pool objects.
 * <p/>
 * This class should not be created directly.  It is targeted for extending.
 *
 * @param <T> type of the pool
 */
public class ForwardingPool<T> extends ForwardingService implements Pool<T> {
  private final Pool<T> pool;

  /**
   * Create a new ForwardingPool that forwards all requests to the give pool
   *
   * @param pool pool to forward to
   */
  public ForwardingPool(final Pool<T> pool) {
    this.pool = Preconditions.checkNotNull(pool);
  }

  @Override
  public Optional<T> borrow() {
    return pool.borrow();
  }

  @Override
  public Optional<T> borrow(final long timeout, final TimeUnit unit) {
    return pool.borrow(timeout, unit);
  }

  @Override
  public void returnToPool(final T obj) {
    pool.returnToPool(obj);
  }

  @Override
  public void returnToPool(final T obj, final Throwable throwable) {
    pool.returnToPool(obj, throwable);
  }

  @Override
  public int getCorePoolSize() {
    return pool.getCorePoolSize();
  }

  @Override
  public int getMaxPoolSize() {
    return pool.getMaxPoolSize();
  }

  @Override
  public int getActivePoolSize() {
    return pool.getActivePoolSize();
  }

  @Override
  public int size() {
    return pool.size();
  }

  @Override
  public boolean isEmpty() {
    return pool.isEmpty();
  }

  @Override
  protected Service delegate() {
    return pool;
  }
}

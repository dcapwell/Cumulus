package com.ekaqu.cunulus.pool;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ForwardingService;
import com.google.common.util.concurrent.Service;

import java.util.concurrent.TimeUnit;

public class ForwardingPool<T> extends ForwardingService implements Pool<T> {
  private final Pool<T> pool;

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
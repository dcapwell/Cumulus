package com.ekaqu.cunulus.pool;

import com.google.common.base.Supplier;

public interface ObjectFactory<T> extends Supplier<T> {

  /**
   * Determines if a Pool should re-add the object to the pool.
   * @param object to add to the pool or not
   * @return true if should add to pool, else false
   */
  boolean shouldAddToPool(T object);

  void cleanup(T obj);
}

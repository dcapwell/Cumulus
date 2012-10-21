package com.ekaqu.cunulus.pool;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Beta
public interface KeyedPool<K, V> extends Pool<Map.Entry<K, V>> {

  /**
   * This is a non-blocking operation that returns a element from the pool.  If no objects are in the pool
   * then the returned value will be {@code Optional#absent()}
   *
   * @throws ClosedPoolException      pool is closed
   * @throws IllegalArgumentException if key doesn't exist
   */
  Optional<Map.Entry<K, V>> borrow(K key);

  /**
   * This is a blocking operation that returns a element from the pool.  A timeout is given to know how long this
   * method is allowed to block for.  If the time has exceeded then the returned value will be empty.
   *
   * @throws ClosedPoolException      pool is closed
   * @throws IllegalArgumentException if key doesn't exist
   */
  Optional<Map.Entry<K, V>> borrow(K key, long timeout, TimeUnit unit);
}

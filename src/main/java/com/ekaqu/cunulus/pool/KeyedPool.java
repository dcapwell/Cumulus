package com.ekaqu.cunulus.pool;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Defines a Pool that is essentially a map of {@code Key -> Pool<V>}.
 * <p/>
 * A KeyedPool is also a pool and should act just like any pool, so methods like {@link com.ekaqu.cunulus.pool.Pool#borrow()}
 * should return a valid value.
 *
 * @param <K> key type
 * @param <V> pool value type
 */
@Beta
public interface KeyedPool<K, V> extends Pool<Map.Entry<K, V>> {

  /**
   * This is a non-blocking operation that returns a element from the pool.  If no objects are in the pool
   * then the returned value will be {@code Optional#absent()}
   * <p/>
   * This method should run between O(1) and O(n)
   *
   * @param key key to use
   * @throws ClosedPoolException      pool is closed
   * @throws IllegalArgumentException if key doesn't exist
   */
  //TODO should a non existing key return Optional#absent?
  Optional<Map.Entry<K, V>> borrow(K key);

  /**
   * This is a blocking operation that returns a element from the pool.  A timeout is given to know how long this
   * method is allowed to block for.  If the time has exceeded then the returned value will be empty.
   * <p/>
   * This method should run between O(1) and O(n)
   *
   * @param key key to use
   * @throws ClosedPoolException      pool is closed
   * @throws IllegalArgumentException if key doesn't exist
   */
  //TODO should a non existing key return Optional#absent?
  Optional<Map.Entry<K, V>> borrow(K key, long timeout, TimeUnit unit);
}

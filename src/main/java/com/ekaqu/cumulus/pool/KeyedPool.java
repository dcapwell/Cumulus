package com.ekaqu.cumulus.pool;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Defines a Pool that is essentially a map of {@code Key -> Pool<V>}.
 * <p/>
 * A KeyedPool is also a pool and should act just like any pool, so methods like {@link
 * com.ekaqu.cumulus.pool.Pool#borrow()} should return a valid value.
 * <p/>
 * It is recommended that while working with pools that the {@link ExecutingPool} is used with a {@link
 * com.ekaqu.cumulus.retry.Retryer}.  This combination will handle most error cases for you.  Also it is best to use the
 * {@link PoolBuilder} for creating pools. The builder always returns a thread safe pool and sane defaults.
 * <p/>
 * Example building with the {@link PoolBuilder}
 * <pre>
 * {@code
 * ExecutingPool<Map.Entry<K, V>> pool;
 * pool = new PoolBuilder<K>()
 *       .corePoolSize(2) // optional size the pool should try to stay around
 *       .maxPoolSize(4) // optional max size the pool can reach
 *       .executorService(executorService) // optional executor service for
 *       .withKeyType(K.class)
 *          .coreSizePerKey(2) // optional size the underlining pools should stay around
 *          .maxSizePerKey(4) // optional max size each underline pool can reach
 *          .keySupplier(keySupplier) // creates new keys that index underline pools
 *          .factory(poolObjFactory) // creates objects for new pools
 *          .buildExecutingPool(Retryers.newExponentialBackoffRetryer(10));
 * }
 * </pre>
 * <p/>
 * A simpler example
 * <pre>
 * {@code
 * ExecutingPool<Map.Entry<K, V>> pool;
 * pool = new PoolBuilder<K>()
 *       .withKeyType(K.class)
 *          .keySupplier(keySupplier) // creates new keys that index underline pools
 *          .factory(poolObjFactory) // creates objects for new pools
 *          .buildExecutingPool(Retryers.newExponentialBackoffRetryer(10));
 * }
 * </pre>
 *
 * @param <K> key type
 * @param <V> pool value type
 */
@Beta
public interface KeyedPool<K, V> extends Pool<Map.Entry<K, V>> {

  /**
   * This is a non-blocking operation that returns a element from the pool.  If no objects are in the pool then the
   * returned value will be {@code Optional#absent()}
   * <p/>
   * This method should run between O(1) and O(n)
   *
   * @param key key to use
   * @return optional entry
   * @throws ClosedPoolException      pool is closed
   * @throws IllegalArgumentException if key doesn't exist
   */
  //TODO should a non existing key return Optional#absent?
  Optional<Map.Entry<K, V>> borrow(K key);

  /**
   * This is a blocking operation that returns a element from the pool.  A timeout is given to know how long this method
   * is allowed to block for.  If the time has exceeded then the returned value will be empty.
   * <p/>
   * This method should run between O(1) and O(n)
   *
   * @param key     key to use
   * @param timeout how long to wait for items if pool is empty
   * @param unit    used for timeout
   * @return optional entry
   * @throws ClosedPoolException      pool is closed
   * @throws IllegalArgumentException if key doesn't exist
   */
  //TODO should a non existing key return Optional#absent?
  Optional<Map.Entry<K, V>> borrow(K key, long timeout, TimeUnit unit);
}

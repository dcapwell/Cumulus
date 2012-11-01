package com.ekaqu.cumulus.pool;

import com.google.common.base.Optional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Forwards all requests to a given KeyedPool.  Use this to aid in decorating pool objects.
 * <p/>
 * This class should not be created directly.  It is targeted for extending.
 *
 * @param <K> key type of the pool
 * @param <V> value type of the pool
 */
public abstract class ForwardingKeyedPool<K, V> extends ForwardingPool<Map.Entry<K, V>> implements KeyedPool<K, V> {

  /**
   * Underline pool to forward to.
   */
  private final KeyedPool<K, V> pool;

  /**
   * Creates a new ForwardingKeyedPool that forwards all requests to the given pool.
   *
   * @param pool to forward to
   */
  public ForwardingKeyedPool(final KeyedPool<K, V> pool) {
    super(pool); // this should check that pool is not null
    this.pool = pool;
  }

  @Override
  public Optional<Map.Entry<K, V>> borrow(final K key) {
    return pool.borrow(key);
  }

  @Override
  public Optional<Map.Entry<K, V>> borrow(final K key, final long timeout, final TimeUnit unit) {
    return pool.borrow(key, timeout, unit);
  }
}

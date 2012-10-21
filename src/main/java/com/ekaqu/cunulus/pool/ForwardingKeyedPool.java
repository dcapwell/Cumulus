package com.ekaqu.cunulus.pool;

import com.google.common.base.Optional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ForwardingKeyedPool<K,V> extends ForwardingPool<Map.Entry<K, V>> implements KeyedPool<K,V> {
  private final KeyedPool<K, V> pool;

  public ForwardingKeyedPool(final KeyedPool<K, V> pool) {
    super(pool);
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

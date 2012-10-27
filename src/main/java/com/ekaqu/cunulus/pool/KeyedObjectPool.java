package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.loadbalancer.CollectionLoadBalancer;
import com.ekaqu.cunulus.loadbalancer.RoundRobinLoadBalancer;
import com.ekaqu.cunulus.util.Factory;
import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Basic KeyedPool for generic objects.
 * <p/>
 * This class is marked thread safe because most methods are.  {@link KeyedObjectPool#setPoolSizes(int, int)} is not
 * thread safe and should be the only unsafe method.
 * <p/>
 * This pool only expands in size if each underline pool is empty
 *
 * @param <K> key type
 * @param <V> value type
 */
//TODO find a way to replace keySupplier, and factory... don't like them
//TODO need more concurrent testing and review
@ThreadSafe
@Beta
public class KeyedObjectPool<K, V> extends AbstractPool<Map.Entry<K, V>> implements KeyedPool<K, V> {

  //TODO should this be configurable?
  private final Predicate<Map.Entry<K, Pool<V>>> poolSizePredicate = new Predicate<Map.Entry<K, Pool<V>>>() {
    @Override
    public boolean apply(@Nullable final Map.Entry<K, Pool<V>> input) {
      if (input == null) return false;
      final Pool<V> pool = input.getValue();
      return !pool.isEmpty() || pool.getActivePoolSize() < pool.getMaxPoolSize();
    }
  };

  //TODO should poolMap and/ore loadBalancer be configurable?
  private final Map<K, Pool<V>> poolMap = Maps.newConcurrentMap();
  private final CollectionLoadBalancer<Map.Entry<K, Pool<V>>> loadBalancer =
      new CollectionLoadBalancer(poolMap.entrySet(), new RoundRobinLoadBalancer(), poolSizePredicate);

  /**
   * When the pool is empty, used to wait for expansion or returned data.
   */
  private final Lock lock = new ReentrantLock();
  private final Condition notEmpty = lock.newCondition();

  /**
   * Used to synchronize pool expansion
   */
  private final Object expandingLock = new Object();

  /**
   * Used to expand the pool in the background
   */
  private final Runnable expandPool = new Runnable() {
    @Override
    public void run() {
      expand();
    }
  };

  @GuardedBy("expandingLock")
  private final Supplier<K> keySupplier;

  @GuardedBy("expandingLock")
  private final Factory<K, ObjectFactory<V>> factory;

  private final ExecutorService executorService;
  private final int coreSizePerKey, maxSizePerKey;

  /**
   * Creates a new KeyedObjectPool
   *
   * @param keySupplier     creates new keys
   * @param factory         creates new ObjectFactory based of keys from keySupplier
   * @param executorService used for async internal logic
   * @param coreSize        min size of active keys
   * @param maxSize         max size of active keys
   * @param coreSizePerKey  min size of objects in sub pools
   * @param maxSizePerKey   max size of objects in sub pools
   */
  public KeyedObjectPool(final Supplier<K> keySupplier,
                         final Factory<K, ObjectFactory<V>> factory,
                         final ExecutorService executorService,
                         final int coreSize, final int maxSize, final int coreSizePerKey, final int maxSizePerKey) {
    this.keySupplier = Preconditions.checkNotNull(keySupplier);
    this.factory = Preconditions.checkNotNull(factory);
    this.executorService = Preconditions.checkNotNull(executorService);

    this.coreSizePerKey = coreSizePerKey;
    this.maxSizePerKey = maxSizePerKey;

    setPoolSizes(coreSize, maxSize);
  }

  @Override
  public Optional<Map.Entry<K, V>> borrow(final long timeout, final TimeUnit unit) {
    Map.Entry<K, Pool<V>> entry = loadBalancer.get();
    if (entry == null) {
      // all pools are at max size!, need to expand this pool
      // try to expand pool size if can
      int poolSize = poolMap.size(), maxPoolSize = super.getMaxPoolSize();
      if (poolSize < maxPoolSize) {
        // leaving dead variable hear to be clear that this is a background operation
        Future<?> future = executorService.submit(expandPool);
      }
      awaitAdded(timeout, unit);
      if (isEmpty()) {
        return Optional.absent();
      } else {
        return borrow();
      }
    }
    return borrow(entry.getValue(), entry.getKey(), timeout, unit);
  }

  @Override
  public Optional<Map.Entry<K, V>> borrow(final K key) {
    return borrow(key, 0, TimeUnit.MILLISECONDS);
  }

  @Override
  public Optional<Map.Entry<K, V>> borrow(final K key, final long timeout, final TimeUnit unit) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(unit);

    checkNotClosed();

    Pool<V> pool = poolMap.get(key);
    return borrow(pool, key, timeout, unit);
  }

  private Optional<Map.Entry<K, V>> borrow(final Pool<V> pool, final K key, final long timeout, final TimeUnit unit) {
    Optional<Map.Entry<K, V>> ret = Optional.absent();
    if (pool != null) {
      Optional<V> value = pool.borrow(timeout, unit);
      if (value.isPresent()) {
        ret = Optional.<Map.Entry<K, V>>of(new AbstractMap.SimpleEntry<K, V>(key, value.get()));
      }
    }
    return ret;
  }

  @Override
  public void returnToPool(final Map.Entry<K, V> obj, final Throwable throwable) {
    final K key = Preconditions.checkNotNull(obj.getKey());
    final V object = Preconditions.checkNotNull(obj.getValue());

    checkNotClosed();

    Pool<V> pool = poolMap.get(key);
    if (pool != null) {
      pool.returnToPool(object, throwable);
      if(pool.isRunning()) {
        notifyAdded();
      } else {
        // pool is closing or is closed, so remove
        poolMap.remove(key);
      }
    } else {
      //TODO should pool be enhanced to support this?
      throw new IllegalArgumentException("Key " + key + " doesn't have a pool");
    }
  }

  @Override
  public String toString() {
    return toStringBuilder()
        .add("keyCount", poolMap.size())
        .add("corePoolSizePerKey", coreSizePerKey)
        .add("maxPoolSizePerKey", maxSizePerKey)
        .add("poolMap", poolMap)
        .toString();
  }

  /**
   * If the pool has room to expand, this method will attempt to expand.  There are three things that make this
   * a slow operation:
   * <p/>
   * <ul>
   * <li>{@link Supplier} used for generating Keys.  This has no expected time and may be a network call</li>
   * <li>{@link Factory} that generates the {@link ObjectFactory}.  This has no expected time and may be a network
   * call</li>
   * <li>{@link com.ekaqu.cunulus.pool.PoolBuilder#build()} that creates a new pool. This has no expected time and
   * may be a network call N times, where N is the coreSizePerKey</li>
   * </ul>
   *
   * @return if pool expanded
   */
  @Override
  protected boolean createAndAdd() {
    boolean added = false;
    Pool<V> oldPool = null;
    synchronized (expandingLock) {
      int poolSize = poolMap.size(),
          maxPoolSize = super.getMaxPoolSize();
      if (poolSize < maxPoolSize) {
        // key supplier may be slow
        K key = Preconditions.checkNotNull(keySupplier.get());
        if (!this.poolMap.containsKey(key)) {
          // factory may be slow
          ObjectFactory<V> poolFactory = Preconditions.checkNotNull(factory.get(key));

          // can be really slow since it requires coreSizePerKey number of Supplier calls
          Pool<V> pool = new PoolBuilder<V>()
              .objectFactory(poolFactory)
              .executorService(executorService)
              .corePoolSize(coreSizePerKey)
              .maxPoolSize(maxSizePerKey).build();


          oldPool = this.poolMap.put(key, pool);
          added = true;
          notifyAdded();
        }
      }
    }
    if (oldPool != null) {
      oldPool.stop(); // let the kill run in the background, don't care about state
    }
    return added;
  }

  /**
   * Causes a wait until a new element has been added.  This should be triggered when an item is returned to the
   * pool or when the pool expands
   *
   * @param timeout how long to wait
   * @param unit    unit that the timeout is using
   */
  private void awaitAdded(final long timeout, final TimeUnit unit) {
    lock.lock();
    try {
      notEmpty.await(timeout, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Notifies that an element has been returned to the pool or the pool expanded.  This should waken callers
   * of {@link KeyedObjectPool#awaitAdded(long, java.util.concurrent.TimeUnit)}
   */
  private void notifyAdded() {
    lock.lock();
    try {
      notEmpty.signal();
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected int shrink(final int shrinkBy) {
    int removed = 0;
    Iterator<Map.Entry<K, Pool<V>>> it = poolMap.entrySet().iterator();
    for (int i = 0; i < shrinkBy && it.hasNext(); i++) {
      Map.Entry<K, Pool<V>> next = it.next();
      Pool<V> pool = next.getValue();
      it.remove();
      pool.stopAndWait();
      removed++;
    }
    return removed;
  }

  @Override
  protected void clear() {
    final List<ListenableFuture<State>> futures = Lists.newArrayList();
    for (final Pool<V> pool : poolMap.values()) {
      futures.add(pool.stop());
    }
    poolMap.clear();
    ListenableFuture<List<State>> result = Futures.successfulAsList(futures);
    try {
      // if the states are not TERMINATED then there isn't really anything to do, so just ignore
      result.get();
    } catch (Exception e) {
      // unable to clean up all the pools, log that this happened
      e.printStackTrace();
    }
  }

  /**
   * The size of all pools added together
   */
  @Override
  public int size() {
    int size = 0;
    for (final Pool<V> pool : poolMap.values()) {
      size += pool.size();
    }
    return size;
  }

  @Override
  public int getActivePoolSize() {
    int size = 0;
    for (final Pool<V> pool : poolMap.values()) {
      size += pool.getActivePoolSize();
    }
    return size;
  }

  @Override
  public int getCorePoolSize() {
    return coreSizePerKey * super.getCorePoolSize();
  }

  @Override
  public int getMaxPoolSize() {
    return maxSizePerKey * super.getMaxPoolSize();
  }

  /**
   * Checks if all pools are empty or not
   *
   * @return true if at least one pool contains an object
   */
  @Override
  public boolean isEmpty() {
    for (final Pool<V> pool : poolMap.values()) {
      if (!pool.isEmpty()) return false;
    }
    return true;
  }
}

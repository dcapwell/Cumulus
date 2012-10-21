package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.util.Factory;
import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Beta
public class KeyedObjectPool<K, V> extends AbstractPool<Map.Entry<K, V>> implements KeyedPool<K, V> {

  private final Map<K, Pool<V>> poolMap = Maps.newConcurrentMap();

  private final Supplier<K> hostSupplier;
  private final Factory<K, ObjectFactory<V>> factory;
  private final KeyChooser<K,V> chooser;
  private final ExecutorService executorService;
  private final int coreSizePerKey, maxSizePerKey;

  public KeyedObjectPool(final Supplier<K> hostSupplier,
                         final Factory<K, ObjectFactory<V>> factory,
                         final ExecutorService executorService,
                         final KeyChooser<K, V> chooser,
                         final int coreSize, final int maxSize, final int coreSizePerKey, final int maxSizePerKey) {
    this.chooser = Preconditions.checkNotNull(chooser);
    this.hostSupplier = Preconditions.checkNotNull(hostSupplier);
    this.factory = Preconditions.checkNotNull(factory);
    this.executorService = Preconditions.checkNotNull(executorService);

    this.coreSizePerKey = coreSizePerKey;
    this.maxSizePerKey = maxSizePerKey;

    setPoolSizes(coreSize, maxSize);
  }

  public KeyedObjectPool(final Supplier<K> hostSupplier,
                         final Factory<K, ObjectFactory<V>> factory,
                         final ExecutorService executorService,
                         final int coreSize, final int maxSize, final int coreSizePerKey, final int maxSizePerKey) {
    this(hostSupplier, factory, executorService, KeyedObjectPool.<K,V>roundRobinKeyChooser(),
        coreSize, maxSize, coreSizePerKey, maxSizePerKey);
  }

  @Override
  public Optional<Map.Entry<K, V>> borrow(final long timeout, final TimeUnit unit) {
    Set<Map.Entry<K, Pool<V>>> set = poolMap.entrySet();
    if(set == null || set.isEmpty()) {
      return Optional.absent();
    } else {
      K key = chooser.choose(set);
      if(key == null) {
        //TODO this doesn't respect the timeout, so this exits early
        return Optional.absent();
      } else {
        return borrow(key, timeout, unit);
      }
    }
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
    final K hostPort = Preconditions.checkNotNull(obj.getKey());
    final V object = Preconditions.checkNotNull(obj.getValue());

    Pool<V> pool = poolMap.get(hostPort);
    if (pool != null) {
      pool.returnToPool(object, throwable);
    } else {
      //TODO should pool be enhanced to support this?
      throw new IllegalArgumentException("Host " + hostPort + " doesn't have a pool");
    }
  }

  @Override
  public String toString() {
    return toStringBuilder()
        .add("corePoolSizePerKey", coreSizePerKey)
        .add("maxPoolSizePerKey", maxSizePerKey)
        .add("poolMap", poolMap)
        .toString();
  }

  @Override
  protected boolean createAndAdd() {
    boolean added = false;
    K key = Preconditions.checkNotNull(hostSupplier.get());
    if (!this.poolMap.containsKey(key)) {
      ObjectFactory<V> poolFactory = Preconditions.checkNotNull(factory.get(key));

      Pool<V> pool = new ObjectPool<V>(poolFactory, executorService, coreSizePerKey, maxSizePerKey);
      pool.startAndWait();

      Pool<V> oldPool = this.poolMap.put(key, pool);
      if (oldPool != null) {
        oldPool.stopAndWait();
      } else {
        added = true;
      }
    }
    return added;
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
      List<State> statuses = result.get();
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

  /**
   * Checks if all pools are empty or not
   *
   * @return true if at least one pool contains an object
   */
  @Override
  public boolean isEmpty() {
    boolean empty = false;
    for (final Pool<V> pool : poolMap.values()) {
      empty |= pool.isEmpty();
    }
    return empty;
  }

  /**
   * Choose a key given a set of entries
   *
   * This may be slow since it involves many iterations.  A faster solution may be needed.
   * @param <K> Key
   * @param <V> Value
   */
  @Beta
  public interface KeyChooser<K,V> {

    /**
     * Choose a key given a set of entries
     * @param values A non empty set of map values
     * @return a valid key from the set
     */
    K choose(Set<Map.Entry<K, Pool<V>>> values);
  }

  public static abstract class AbstractKeyChooser<K, V> implements KeyChooser<K, V> {

    private Predicate<Map.Entry<K, Pool<V>>> predicate = EntryPredicates.NON_EMPTY.withNarrowedType();

    @Override
    public K choose(final Set<Map.Entry<K, Pool<V>>> values) {
      Collection<Map.Entry<K, Pool<V>>> filtered = Collections2.filter(values, predicate);
      return filtered(filtered);
    }

    public Predicate<Map.Entry<K, Pool<V>>> getPredicate() {
      return predicate;
    }

    public void setPredicate(final Predicate<Map.Entry<K, Pool<V>>> predicate) {
      this.predicate = Preconditions.checkNotNull(predicate);
    }

    protected abstract K filtered(final Collection<Map.Entry<K, Pool<V>>> filtered);

    enum EntryPredicates implements Predicate<Map.Entry<Object, Pool<Object>>> {
      NON_EMPTY {
        @Override
        public boolean apply(final Map.Entry<Object, Pool<Object>> input) {
          return ! input.getValue().isEmpty();
        }
      };

      @SuppressWarnings("unchecked") // these Object predicates work for any T
      <T> Predicate<T> withNarrowedType() {
        return (Predicate<T>) this;
      }
    }
  }

  public static <K,V> KeyChooser<K, V> roundRobinKeyChooser() {
    return new AbstractKeyChooser<K, V>() {
      private final AtomicInteger index = new AtomicInteger(0);

      @Override
      protected K filtered(final Collection<Map.Entry<K, Pool<V>>> filtered) {
        if(filtered.size() == 0) {
          return null;
        }
        int thisIndex = Math.abs(index.getAndIncrement());
        Map.Entry<K, Pool<V>> entry = Iterables.get(filtered, thisIndex % filtered.size());
        return entry.getKey();
      }
    };
  }
}

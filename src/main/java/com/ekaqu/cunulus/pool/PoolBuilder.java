package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.retry.Retryer;
import com.ekaqu.cunulus.util.Factory;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Helps build {@link Pool} objects with a <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent interface</a>
 * and sensible defaults.
 * <p/>
 * Example building a {@link ExecutingPool} backed by a {@link Pool} using the builder
 * <pre>
 * {@code
 * ExecutingPool<T> pool = new PoolBuilder<T>()
 *                    .objectFactory(poolValueFactory) // defines how to create new pooled entities
 *                    .executorService(executorService) // optional executor service for background pool operations
 *                    .corePoolSize(2) // optional size the pool should try to stay around
 *                    .maxPoolSize(4) // optional max size the pool can reach
 *                    .buildExecutingPool(Retryers.newExponentialBackoffRetryer(10));
 * }
 * </pre>
 * <p/>
 * A simpler example
 * <pre>
 * {@code
 * ExecutingPool<T> pool = new PoolBuilder<T>()
 *                    .objectFactory(poolValueFactory) // defines how to create new pooled entities
 *                    .buildExecutingPool(Retryers.newExponentialBackoffRetryer(10));
 * }
 * </pre>
 * <p/>
 * How to build a {@link ExecutingPool} backed by {@link KeyedPool}
 * <pre>
 * {@code
 * ExecutingPool<Map.Entry<K,V>> pool = new PoolBuilder<K>()
 *                                          .corePoolSize(2) // optional size the pool should try to stay around
 *                                          .maxPoolSize(4) // optional max size the pool can reach
 *                                          .executorService(executorService) // optional executor service for
 * background pool operations
 *                                          .withKeyType(String.class)
 *                                              .coreSizePerKey(2) // optional size the underlining pools should stay
 * around
 *                                              .maxSizePerKey(4) // optional max size each underline pool can reach
 *                                              .keySupplier(keySupplier) // creates new keys that index underline
 * pools
 *                                              .factory(stringFactory) // creates a new ObjectFactory for the keys
 * which defines how to create new pooled entities
 *                                              .buildExecutingPool(Retryers.newExponentialBackoffRetryer(10));
 * }
 * </pre>
 * <p/>
 * A simpler example building a {@link ExecutingPool} backed by {@link KeyedPool}
 * <pre>
 * {@code
 * ExecutingPool<Map.Entry<K,V>> pool = new PoolBuilder<K>()
 *                                          .withKeyType(String.class)
 *                                              .keySupplier(keySupplier) // creates new keys that index underline
 * pools
 *                                              .factory(stringFactory) // creates a new ObjectFactory for the keys
 * which defines how to create new pooled entities
 *                                              .buildExecutingPool(Retryers.newExponentialBackoffRetryer(10));
 * }
 * </pre>
 *
 * @param <T> type of the pool to build
 */
public final class PoolBuilder<T> {

  /**
   * Creates daemon threads.
   */
  private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setDaemon(true).build();

  /**
   * Default max size a pool can expand to.
   */
  private static final int DEFAULT_MAX_POOL_SIZE = 10;

  /**
   * Default core size a pool should try to stay around.
   */
  private static final int DEFAULT_CORE_POOL_SIZE = DEFAULT_MAX_POOL_SIZE / 2;

  /**
   * Executor service for running background tasks.
   */
  private ExecutorService executorService;

  /**
   * The min size of the pool.
   */
  private int corePoolSize;

  /**
   * The max size the pool can grow to.
   */
  private int maxPoolSize;

  /**
   * Creates values for the pool.
   */
  private ObjectFactory<T> objectFactory;

  /**
   * Core size the pool should try to stay at.
   *
   * @param corePoolSize size the pool should be around
   * @return this builder
   */
  public PoolBuilder<T> corePoolSize(final int corePoolSize) {
    Preconditions.checkArgument(corePoolSize >= 0, "core pool size must be positive or zero");
    this.corePoolSize = corePoolSize;
    return this;
  }

  /**
   * Max size the pool should reach.
   *
   * @param maxPoolSize size the pool can reach.  Pool can not go over this value
   * @return this builder
   */
  public PoolBuilder<T> maxPoolSize(final int maxPoolSize) {
    Preconditions.checkArgument(maxPoolSize >= 0, "max pool size must be positive or zero");
    this.maxPoolSize = maxPoolSize;
    return this;
  }

  /**
   * Factory for creating values in a pool.
   *
   * @param factory how pool should manage values
   * @return this builder
   */
  public PoolBuilder<T> objectFactory(final ObjectFactory<T> factory) {
    this.objectFactory = Preconditions.checkNotNull(factory);
    return this;
  }

  /**
   * ExecutorService pools may use to run background tasks. If no executorService is provided a single threaded executor
   * will be created for the pool
   *
   * @param executorService used for background tasks
   * @return this builder
   */
  public PoolBuilder<T> executorService(final ExecutorService executorService) {
    this.executorService = Preconditions.checkNotNull(executorService);
    return this;
  }

  /**
   * Get the executorService for this pool.
   *
   * @return executor service used by the pool builder, or a new single threaded one
   */
  private ExecutorService getExecutorService() {
    return (executorService == null)
        ? executorService = Executors.newSingleThreadExecutor(THREAD_FACTORY)
        : this.executorService;
  }

  /**
   * Build a new Pool.
   *
   * @return newly created pool
   */
  public Pool<T> build() {
    final ObjectFactory<T> objectFactory = Preconditions.checkNotNull(this.objectFactory);
    ExecutorService executorService = getExecutorService();

    int corePoolSize = this.corePoolSize;
    int maxPoolSize = this.maxPoolSize;

    if (maxPoolSize == 0) {
      maxPoolSize = DEFAULT_MAX_POOL_SIZE;
      if (corePoolSize == 0) {
        corePoolSize = DEFAULT_CORE_POOL_SIZE;
      }
    }

    Pool<T> pool = new ObjectPool<T>(objectFactory, executorService, corePoolSize, maxPoolSize);
    startPool(pool);
    return pool;
  }

  /**
   * Build a new ExecutingPool.
   *
   * @return newly created pool
   */
  public ExecutingPool<T> buildExecutingPool() {
    return ExecutingPool.executor(build());
  }

  /**
   * Build a new ExecutingPool.
   *
   * @param retryer used for retrying {@link ExecutingPool#execute(com.ekaqu.cunulus.util.Block)}
   * @return newly created pool
   */
  public ExecutingPool<T> buildExecutingPool(final Retryer retryer) {
    return ExecutingPool.retryingExecutor(build(), retryer);
  }

  /**
   * Builder for KeyedPools.
   *
   * @param clazz key class type
   * @param <K>   key type
   * @return new KeyedPoolBuilder
   */
  public <K> KeyedPoolBuilder<K, T> withKeyType(final Class<K> clazz) {
    return new KeyedPoolBuilder<K, T>();
  }

  /**
   * Builder for Keyed pools.  Should not use this class directly but from {@link PoolBuilder#withKeyType(Class)}
   *
   * @param <K> key type for pool
   * @param <V> value type of pool.  This should be provided by {@link PoolBuilder}
   */
  public final class KeyedPoolBuilder<K, V> {

    /**
     * Creates keys for the pool.
     */
    private Supplier<K> keySupplier;

    /**
     * Creates new ObjectFactorys given the key from {@link #keySupplier}.
     */
    private Factory<K, ObjectFactory<V>> factory;

    /**
     * Min size of the underline pools.
     */
    private int coreSizePerKey;

    /**
     * Max size of the underline pools.
     */
    private int maxSizePerKey;

    /**
     * Hides constructor from clients.
     */
    private KeyedPoolBuilder() {
    }

    /**
     * Supplier for generating keys for the pool.
     *
     * @param keySupplier for generating keys
     * @return this builder
     */
    public KeyedPoolBuilder<K, V> keySupplier(final Supplier<K> keySupplier) {
      this.keySupplier = Preconditions.checkNotNull(keySupplier);
      return this;
    }

    /**
     * Factory for creating ObjectFactorys needed for building a new Pool for a given Key.
     *
     * @param factory for creating ObjectFactoriys
     * @return this builder
     */
    public KeyedPoolBuilder<K, V> factory(final Factory<K, ObjectFactory<V>> factory) {
      this.factory = Preconditions.checkNotNull(factory);
      return this;
    }

    /**
     * What the core pool size should be for each generated pool inside the KeyedPool.
     *
     * @param coreSizePerKey coreSize for each pool within the KeyedPool
     * @return this builder
     */
    public KeyedPoolBuilder<K, V> coreSizePerKey(final int coreSizePerKey) {
      Preconditions.checkArgument(coreSizePerKey >= 0, "core pool size must be positive or zero");
      this.coreSizePerKey = coreSizePerKey;
      return this;
    }

    /**
     * What the max pool size should be for each generated pool inside the KeyedPool.
     *
     * @param maxSizePerKey maxSize for each pool within the KeyedPool
     * @return this builder
     */
    public KeyedPoolBuilder<K, V> maxSizePerKey(final int maxSizePerKey) {
      Preconditions.checkArgument(maxSizePerKey >= 0, "max pool size must be positive or zero");
      this.maxSizePerKey = maxSizePerKey;
      return this;
    }

    /**
     * Build a new KeyedPool.
     *
     * @return newly created KeyedPool
     */
    public KeyedPool<K, V> build() {
      final Supplier<K> hostSupplier = Preconditions.checkNotNull(this.keySupplier);
      final Factory<K, ObjectFactory<V>> factory = Preconditions.checkNotNull(this.factory);
      final ExecutorService executorService = getExecutorService();

      int corePoolSize = PoolBuilder.this.corePoolSize;
      int maxPoolSize = PoolBuilder.this.maxPoolSize;

      if (maxPoolSize == 0) {
        maxPoolSize = DEFAULT_MAX_POOL_SIZE;
        if (corePoolSize == 0) {
          corePoolSize = DEFAULT_CORE_POOL_SIZE;
        }
      }

      int corePoolSizePerKey = coreSizePerKey;
      int maxPoolSizePerKey = maxSizePerKey;

      if (maxPoolSizePerKey == 0) {
        maxPoolSizePerKey = maxPoolSize;
        if (corePoolSizePerKey == 0) {
          corePoolSizePerKey = corePoolSize;
        }
      }

      KeyedPool<K, V> pool = new KeyedObjectPool<K, V>(hostSupplier, factory, executorService,
          corePoolSize, maxPoolSize, corePoolSizePerKey, maxPoolSizePerKey);
      startPool(pool);
      return pool;
    }

    /**
     * Build a new ExecutingPool backed by a KeyedPool.
     *
     * @return newly created executingPool
     */
    public ExecutingPool<Map.Entry<K, V>> buildExecutingPool() {
      return ExecutingPool.executor(build());
    }

    /**
     * Build a new ExecutingPool backed by a KeyedPool with retires\ for {@link ExecutingPool#execute(com.ekaqu.cunulus.util.Block)}.
     *
     * @param retryer for retry logic in execute
     * @return newly created executingPool
     */
    public ExecutingPool<Map.Entry<K, V>> buildExecutingPool(final Retryer retryer) {
      return ExecutingPool.retryingExecutor(build(), retryer);
    }
  }

  /**
   * Start a pool and verify its in a good state.
   *
   * @param pool newly created pool to start
   */
  private static void startPool(final Pool<?> pool) {
    Service.State state = pool.startAndWait();
    switch (state) {
      case FAILED:
        pool.stop();
        throw new IllegalStateException("Unable to build pool");
      case NEW:
      case STARTING:
      case STOPPING:
      case TERMINATED:
        throw new AssertionError();
      case RUNNING:
        // should be good
        break;
      default:
        throw new AssertionError();
    }
  }
}

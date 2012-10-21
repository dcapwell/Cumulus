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
 * Helps build Pool objects with a streamlined interface and sensible defaults.
 *
 * Example of a basic object pool
 * {@code
Pool<T> pool = new PoolBuilder<T>()
  .objectFactory(factory)
  .build();
}
 *
 * This class can also be used to build KeyedPools
 * {@code
KeyedPool<K, V> pool = new PoolBuilder<K>()
  .withKeyType(V.class)
    .keySupplier(factory)
    .factory(factoryFactory)
    .build();
}
 * @param <T>
 */
public class PoolBuilder<T> {
  private static final ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).build();

  private static final int DEFAULT_MAX_POOL_SIZE = 10;
  private static final int DEFAULT_CORE_POOL_SIZE = DEFAULT_MAX_POOL_SIZE / 2;

  private ExecutorService executorService;
  private int corePoolSize;
  private int maxPoolSize;
  private ObjectFactory<T> objectFactory;

  public PoolBuilder<T> corePoolSize(final int corePoolSize) {
    Preconditions.checkArgument(corePoolSize >= 0, "core pool size must be positive or zero");
    this.corePoolSize = corePoolSize;
    return this;
  }

  public PoolBuilder<T> maxPoolSize(final int maxPoolSize) {
    Preconditions.checkArgument(maxPoolSize >= 0, "max pool size must be positive or zero");
    this.maxPoolSize = maxPoolSize;
    return this;
  }

  public PoolBuilder<T> objectFactory(final ObjectFactory<T> factory) {
    this.objectFactory = Preconditions.checkNotNull(factory);
    return this;
  }

  public PoolBuilder<T> executorService(final ExecutorService executorService) {
    this.executorService = Preconditions.checkNotNull(executorService);
    return this;
  }

  private ExecutorService getExecutorService() {
    return (executorService == null) ? executorService = Executors.newSingleThreadExecutor(threadFactory) : this.executorService;
  }

  public Pool<T> build() {
    final ObjectFactory<T> objectFactory = Preconditions.checkNotNull(this.objectFactory);
    ExecutorService executorService = getExecutorService();

    int corePoolSize = this.corePoolSize;
    int maxPoolSize = this.maxPoolSize;

    if(maxPoolSize == 0) {
      maxPoolSize = DEFAULT_MAX_POOL_SIZE;
      if(corePoolSize == 0) {
        corePoolSize = DEFAULT_CORE_POOL_SIZE;
      }
    }

    Pool<T> pool = new ObjectPool<T>(objectFactory, executorService, corePoolSize, maxPoolSize);
    startPool(pool);
    return pool;
  }

  public ExecutingPool<T> buildExecutingPool() {
    return ExecutingPool.executor(build());
  }

  public ExecutingPool<T> buildExecutingPool(final Retryer retryer) {
    return ExecutingPool.retryingExecutor(build(), retryer);
  }

  public <K> KeyedPoolBuilder<K, T> withKeyType(Class<K> clazz) {
    return new KeyedPoolBuilder<K, T>();
  }

  public class KeyedPoolBuilder<K, V> {

    private Supplier<K> keySupplier;
    private Factory<K, ObjectFactory<V>> factory;
    private KeyedObjectPool.KeyChooser<K,V> chooser;
    private int coreSizePerKey;
    private int maxSizePerKey;

    public KeyedPoolBuilder<K,V> keySupplier(final Supplier<K> keySupplier) {
      this.keySupplier = Preconditions.checkNotNull(keySupplier);
      return this;
    }

    public KeyedPoolBuilder<K,V> factory(final Factory<K, ObjectFactory<V>> factory) {
      this.factory = Preconditions.checkNotNull(factory);
      return this;
    }

    public KeyedPoolBuilder<K,V> keyChooser(final KeyedObjectPool.KeyChooser<K, V> chooser) {
      this.chooser = Preconditions.checkNotNull(chooser);
      return this;
    }

    public KeyedPoolBuilder<K,V> coreSizePerKey(final int coreSizePerKey) {
      Preconditions.checkArgument(coreSizePerKey >= 0, "core pool size must be positive or zero");
      this.coreSizePerKey = coreSizePerKey;
      return this;
    }

    public KeyedPoolBuilder<K,V> maxSizePerKey(final int maxSizePerKey) {
      Preconditions.checkArgument(maxSizePerKey >= 0, "max pool size must be positive or zero");
      this.maxSizePerKey = maxSizePerKey;
      return this;
    }

    public KeyedPool<K, V> build() {
      final Supplier<K> hostSupplier = Preconditions.checkNotNull(this.keySupplier);
      final Factory<K, ObjectFactory<V>> factory = Preconditions.checkNotNull(this.factory);
      final ExecutorService executorService = getExecutorService();

      KeyedObjectPool.KeyChooser<K, V> chooser = this.chooser;
      if(chooser == null) {
        chooser = KeyedObjectPool.roundRobinKeyChooser();
      }

      int corePoolSize = PoolBuilder.this.corePoolSize;
      int maxPoolSize = PoolBuilder.this.maxPoolSize;

      if(maxPoolSize == 0) {
        maxPoolSize = DEFAULT_MAX_POOL_SIZE;
        if(corePoolSize == 0) {
          corePoolSize = DEFAULT_CORE_POOL_SIZE;
        }
      }

      int corePoolSizePerKey = coreSizePerKey;
      int maxPoolSizePerKey = maxSizePerKey;

      if(maxPoolSizePerKey == 0) {
        maxPoolSizePerKey = DEFAULT_MAX_POOL_SIZE;
        if(corePoolSizePerKey == 0) {
          corePoolSizePerKey = DEFAULT_CORE_POOL_SIZE;
        }
      }

      KeyedPool<K, V> pool = new KeyedObjectPool<K, V>(hostSupplier, factory, executorService, chooser,
          corePoolSize, maxPoolSize, corePoolSizePerKey, maxPoolSizePerKey);
      startPool(pool);
      return pool;
    }

    public ExecutingPool<Map.Entry<K,V>> buildExecutingPool() {
      return ExecutingPool.executor(build());
    }

    public ExecutingPool<Map.Entry<K,V>> buildExecutingPool(final Retryer retryer) {
      return ExecutingPool.retryingExecutor(build(), retryer);
    }
  }

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

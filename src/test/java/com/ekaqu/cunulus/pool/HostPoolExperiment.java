package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.util.Block;
import com.ekaqu.cunulus.util.Factory;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A host based pool should be a normal pool but each borrow talks to a different pool, a pool belonging to a give host
 */
@Test(groups = "Experiment")
public class HostPoolExperiment {
  private static final Logger LOGGER = LoggerFactory.getLogger(HostPoolExperiment.class.getName());

  /**
   * This needs to have random keyed access for when an object gets returned.
   * Also in order to have borrow partition the load, indexed access to the keys is also needed
   *
   * Also this will need two things:
   * 1) Which host/ports to use (static | dynamic) Supplier(HostAndPort)?
   * 2) create a ObjectFactory based off a host/port
   * @param <T>
   */
  public static class HostBasedPool<T> extends AbstractService implements Pool<HostBasedPool.HostObject<T>> {

    private final Supplier<HostAndPort> hostSupplier;
    private final Factory<HostAndPort, ObjectFactory<T>> factory;
    private final int coreSize, maxSize, coreSizePerHost, maxSizePerHost;
    private final ExecutorService executorService;

    private final Map<HostAndPort, Pool<T>> poolMap;

    public HostBasedPool(final Supplier<HostAndPort> hostSupplier,
                         final Factory<HostAndPort, ObjectFactory<T>> factory,
                         final ExecutorService executorService,
                         final int coreSize, final int maxSize,
                         final int coreSizePerHost, final int maxSizePerHost) {
      this.executorService = Preconditions.checkNotNull(executorService);
      this.hostSupplier = Preconditions.checkNotNull(hostSupplier);
      this.factory = Preconditions.checkNotNull(factory);

      this.coreSize = coreSize;
      this.coreSizePerHost = coreSizePerHost;
      this.maxSize = maxSize;
      this.maxSizePerHost = maxSizePerHost;

      poolMap = Maps.newConcurrentMap();
    }

    @Override
    public int size() {
      int size = 0;
      for(final Pool<T> pool : poolMap.values()) {
        size += pool.size();
      }
      return size;
    }

    @Override
    public boolean isEmpty() {
      boolean empty = false;
      for(final Pool<T> pool : poolMap.values()) {
        empty |= pool.isEmpty();
      }
      return empty;
    }

    @Override
    protected void doStart() {
      Preconditions.checkState(State.STARTING.equals(state()), "Not in the starting state: " + state());
      try {
//        for(int i = 0; i < coreSize && poolMap.size() < coreSize && createAndAdd(); i++) {}
        while (poolMap.size() < coreSize && createAndAdd()) { }
        notifyStarted();
      } catch (Exception e) {
        notifyFailed(e);
      }
    }

    private boolean createAndAdd() {
      boolean added = false;
      //TODO should this lock? There is a chance that the number of objects is larger than max size
      //TODO queue is capped so going larger shouldn't be an issue
      if(maxSize > poolMap.size()) {
        HostAndPort hostAndPort = Preconditions.checkNotNull(hostSupplier.get());
        ObjectFactory<T> poolFactory = Preconditions.checkNotNull(factory.get(hostAndPort));

        Pool<T> pool = new ObjectPool<T>(poolFactory, executorService, coreSizePerHost, maxSizePerHost);
        pool.startAndWait();

        Pool<T> oldPool = this.poolMap.put(hostAndPort, pool);
        added = oldPool == null;
        if(! added) {
          oldPool.startAndWait();
        }
      }
      return added;
    }

    @Override
    protected void doStop() {
      final List<ListenableFuture<State>> futures = Lists.newArrayList();
      for(final Pool<T> pool : poolMap.values()) {
        ListenableFuture<State> status = pool.start();
        futures.add(status);
      }
      ListenableFuture<List<State>> result = Futures.successfulAsList(futures);
      try {
        List<State> statuses = result.get();
        notifyStopped();
      } catch (Exception e) {
        notifyFailed(e);
      }
    }

    @Override
    public Optional<HostObject<T>> borrow() {
      Map.Entry<HostAndPort, Pool<T>> pool = Iterables.get(poolMap.entrySet(), 0);
      if(pool != null) {
        Optional<T> borrowed = pool.getValue().borrow();
        if(borrowed.isPresent()) {
          return Optional.of(new HostObject<T>(pool.getKey(), borrowed.get()));
        }
      }
      return Optional.absent();
    }

    @Override
    public Optional<HostObject<T>> borrow(final long timeout, final TimeUnit unit) {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void returnToPool(final HostObject<T> obj) {
      returnToPool(obj, null);
    }

    @Override
    public void returnToPool(final HostObject<T> obj, final Throwable throwable) {
      final HostAndPort hostAndPort = obj.getHostAndPort();
      final Pool<T> pool = poolMap.get(hostAndPort);
      if(pool != null) {
        pool.returnToPool(obj.getObject(), throwable);
      }
    }

    public static class HostObject<T> {
      private final HostAndPort hostAndPort;
      private final T obj;

      public HostObject(final HostAndPort hostAndPort, final T obj) {
        this.hostAndPort = hostAndPort;
        this.obj = obj;
      }

      public HostAndPort getHostAndPort() {
        return hostAndPort;
      }

      public T getObject() {
        return obj;
      }

      @Override
      public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("HostObject");
        sb.append("{hostAndPort=").append(hostAndPort);
        sb.append(", obj=").append(obj);
        sb.append('}');
        return sb.toString();
      }
    }
  }

  /**
   * To create a new Host backed pool, you need a factory to create a pool for a given HostAndPort
   */
  public void hostPoolFeel() {
    // given
    Supplier<HostAndPort> hostAndPortSupplier = mock(Supplier.class);
    Factory<HostAndPort, ObjectFactory<String>> factory = mock(Factory.class);

    HostBasedPool<String> pool = new HostBasedPool<String>(hostAndPortSupplier, factory,
        MoreExecutors.sameThreadExecutor(), 2, 4, 2, 4);

    ExecutingPool<HostBasedPool.HostObject<String>> executingPool = ExecutingPool.executor(pool);

    // when
    HostAndPort hostAndPort = HostAndPort.fromParts("localhost", 80);
    when(hostAndPortSupplier.get()).thenReturn(hostAndPort, hostAndPort, hostAndPort, hostAndPort, hostAndPort, hostAndPort, hostAndPort, hostAndPort);
    ObjectFactory<String> stringObjectFactory = mock(ObjectFactory.class);
    when(stringObjectFactory.get()).thenReturn("a", "b", "c");
    when(stringObjectFactory.validate(any(String.class), any(Throwable.class))).thenReturn(ObjectFactory.State.VALID);
    when(factory.get(hostAndPort)).thenReturn(stringObjectFactory);

    executingPool.startAndWait();

    executingPool.execute(new Block<HostBasedPool.HostObject<String>>() {
      @Override
      public void apply(final HostBasedPool.HostObject<String> stringHostObject) {
        LOGGER.info("Host Object {}", stringHostObject);
      }
    });
  }

  public void timeUnitToZero() {
    long nanos = TimeUnit.MILLISECONDS.toNanos(0);
    LOGGER.info("Nanos {}", nanos);
    Assert.assertEquals(nanos, 0);
  }
}

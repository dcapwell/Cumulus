package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.util.Block;
import com.ekaqu.cunulus.util.Factory;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;
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
   * To create a new Host backed pool, you need a factory to create a pool for a given HostAndPort
   */
  public void hostPoolFeel() {
    // given
    Supplier<HostAndPort> hostAndPortSupplier = mock(Supplier.class);
    Factory<HostAndPort, ObjectFactory<String>> factory = mock(Factory.class);

    KeyedObjectPool<HostAndPort, String> pool = new KeyedObjectPool<HostAndPort, String>(hostAndPortSupplier, factory,
        MoreExecutors.sameThreadExecutor(), 2, 4, 2, 4);

    ExecutingPool<Map.Entry<HostAndPort, String>> executingPool = ExecutingPool.executor(pool);

    // when
    HostAndPort hostAndPort = HostAndPort.fromParts("localhost", 80);
    when(hostAndPortSupplier.get()).thenReturn(hostAndPort, hostAndPort, hostAndPort, hostAndPort, hostAndPort, hostAndPort, hostAndPort, hostAndPort);
    ObjectFactory<String> stringObjectFactory = mock(ObjectFactory.class);
    when(stringObjectFactory.get()).thenReturn("a", "b", "c");
    when(stringObjectFactory.validate(any(String.class), any(Throwable.class))).thenReturn(ObjectFactory.State.VALID);
    when(factory.get(hostAndPort)).thenReturn(stringObjectFactory);

    executingPool.startAndWait();

    Optional<Map.Entry<HostAndPort, String>> borrowed = executingPool.borrow();
    Optional<Map.Entry<HostAndPort, String>> borrowed2 = executingPool.borrow();

    executingPool.execute(new Block<Map.Entry<HostAndPort, String>>() {
      @Override
      public void apply(final Map.Entry<HostAndPort, String> hostAndPortStringEntry) {
        LOGGER.info("Host Object {}", hostAndPortStringEntry);
      }
    });
  }

  public void timeUnitToZero() {
    long nanos = TimeUnit.MILLISECONDS.toNanos(0);
    LOGGER.info("Nanos {}", nanos);
    Assert.assertEquals(nanos, 0);
  }
}

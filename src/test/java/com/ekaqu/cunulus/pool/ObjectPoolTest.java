package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.pool.mocks.StringObjectFactory;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Test(groups = "Unit")
public class ObjectPoolTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectPoolTest.class.getName());

  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
  private final StringObjectFactory stringFactory = new StringObjectFactory();

  public void startPool() {
    Pool<String> pool = new ObjectPool<String>(new StringFactory(), executorService, 5, 10);
    pool.startAndWait();

    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(pool.size(), 5, "CorePoolSize not set at startup");

    for(int i = 0; i < 100; i++) {
      String obj = pool.borrow().get();
      int index = Integer.parseInt(obj.substring(obj.length() - 1));

      LOGGER.info("Got object {}", obj);
      Assert.assertTrue(index < 5);
      pool.returnToPool(obj, new Throwable());
    }

    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(pool.size(), 5, "CorePoolSize not set at startup");
  }

  public void expandingPool() {
    final Pool<String> pool = new ObjectPool<String>(new StringFactory(), executorService, 5, 10);
    pool.startAndWait();

    for(int i = 0; i < 10; i++) {
      final String obj = pool.borrow(50, TimeUnit.SECONDS).get();

      executorService.schedule(new Runnable() {
        @Override
        public void run() {
          pool.returnToPool(obj, new Throwable());
        }
      }, 500, TimeUnit.MILLISECONDS);
    }

    Assert.assertEquals(pool.size(), 0);

    // wait for objects to return
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    Assert.assertEquals(pool.size(), 10);
  }

  public void concurrentExpandingPool() {
    final Pool<String> pool = new ObjectPool<String>(new StringFactory(), executorService, 5, 10);
    pool.startAndWait();

    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(pool.size(), 5, "CorePoolSize not set at startup");

    for(int i = 0; i < 100; i++) {
      final String obj = pool.borrow(50, TimeUnit.SECONDS).get();

      executorService.schedule(new Runnable() {
        @Override
        public void run() {
          pool.returnToPool(obj, new Throwable());
        }
      }, 5, TimeUnit.MILLISECONDS);
    }

    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(pool.size(), 10, "MaxPoolSize not expanded to");
  }

  public void rejectInvalid() {
    // given
    ObjectFactory<String> factory = mock(ObjectFactory.class);
    final Pool<String> pool = new ObjectPool<String>(factory, executorService, 2, 4);

    // when
    when(factory.get()).thenReturn("one", "two", "three", "four");
    pool.startAndWait();

    // take the elements out of the queue so its empty
    for(int i = 0; i < 2; i++) {
      pool.borrow();
    }

    String num = "five";
    when(factory.validate(num, null)).thenReturn(ObjectFactory.State.INVALID);

    // then
    pool.returnToPool(num, null);
    Assert.assertEquals(pool.size(), 0);
    verify(factory).cleanup(num);
  }

  public void rejectIAndKillPool() {
    // given
    ObjectFactory<String> factory = mock(ObjectFactory.class);
    final Pool<String> pool = new ObjectPool<String>(factory, executorService, 2, 4);

    // when
    when(factory.get()).thenReturn("one", "two", "three", "four");
    pool.startAndWait();
    LOGGER.info("Pool {}", pool);

    // take the elements out of the queue so its empty
    for(int i = 0; i < 2; i++) {
      pool.borrow();
    }

    String num = "five";
    when(factory.validate(num, null)).thenReturn(ObjectFactory.State.CLOSE_POOL);

    // then
    pool.returnToPool(num, null);
    Assert.assertEquals(pool.size(), 0);
    Assert.assertFalse(pool.isRunning(), "Currently running");
  }

  public void addMoreThanMax() {
    // given
    ObjectFactory<String> factory = mock(ObjectFactory.class);
    final Pool<String> pool = new ObjectPool<String>(factory, executorService, 2, 2);

    // when
    when(factory.get()).thenReturn("one", "two", "three", "four");
    pool.startAndWait();
    LOGGER.info("Pool {}", pool);

    String num = "five";
    when(factory.validate(num, null)).thenReturn(ObjectFactory.State.VALID);

    // then
    pool.returnToPool(num, null);
    Assert.assertEquals(pool.size(), 2);
    Assert.assertTrue(pool.isRunning(), "Currently running");
    verify(factory).cleanup(num);
  }

  public void shrinkPool() {
    // given
    ObjectFactory<String> factory = mock(ObjectFactory.class);
    final AbstractPool<String> pool = new ObjectPool<String>(factory, executorService, 4, 4);

    // when
    when(factory.get()).thenReturn("one", "two", "three", "four");
    pool.startAndWait();
    LOGGER.info("Pool {}", pool);

    String num = "five";
    when(factory.validate(num, null)).thenReturn(ObjectFactory.State.VALID);
    pool.setPoolSizes(2, 2);

    // then
    pool.returnToPool(num, null);
    Assert.assertEquals(pool.size(), 2);
    Assert.assertTrue(pool.isRunning(), "Currently running");
  }

  /**
   * Check if the pool is empty at the right stages.
   *
   * empty happens when the pool has no elements in it.  this doesn't mean that the pool can't expand though.
   */
  public void isEmpty() {
    Pool<String> pool = new PoolBuilder<String>()
        .executorService(executorService)
        .corePoolSize(1)
        .maxPoolSize(2)
        .objectFactory(stringFactory)
        .build();

    Assert.assertFalse(pool.isEmpty());
    for(int i = 0, maxSize = pool.getCorePoolSize(); i < maxSize; i++) {
      Assert.assertFalse(pool.isEmpty());
      pool.borrow(5, TimeUnit.SECONDS).get();
    }
    Assert.assertTrue(pool.isEmpty());
  }

  private static class StringFactory extends AbstractObjectFactory<String> {
    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public String get() {
      return "StringFactory-" + count.getAndIncrement();
    }
  }
}

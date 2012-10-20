package com.ekaqu.cunulus.pool;

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

  public void startPool() {
    Pool<String> pool = new ObjectPool<String>(new StringFactory(), 5, 10, executorService);
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
    final Pool<String> pool = new ObjectPool<String>(new StringFactory(), 5, 10, executorService);
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
    final Pool<String> pool = new ObjectPool<String>(new StringFactory(), 5, 10, executorService);
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
      }, 500, TimeUnit.MILLISECONDS);
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
    final Pool<String> pool = new ObjectPool<String>(factory, 2, 4, executorService);

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
    final Pool<String> pool = new ObjectPool<String>(factory, 2, 4, executorService);

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
    final Pool<String> pool = new ObjectPool<String>(factory, 2, 2, executorService);

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
  }

  private static class StringFactory extends AbstractObjectFactory<String> {
    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public String get() {
      return "StringFactory-" + count.getAndIncrement();
    }
  }
}

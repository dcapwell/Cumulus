package com.ekaqu.cunulus.pool;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Test(groups = "Experiment")
public class PoolExperiment {
  private static final Logger LOGGER = LoggerFactory.getLogger(PoolExperiment.class.getName());

  public interface Pool<T> {
    T get();
    void returnConnection(T connection);
  }

  /**
   * Doesn't support controling wait time, so get should always be O(1) or could be an issue
   */
  public void interfaceTest() {
    // given
    Pool<String> objectPool = mock(Pool.class);

    // when
    when(objectPool.get()).thenReturn("Hello");
    String obj = objectPool.get();
    LOGGER.info("Got object {}", obj);
    objectPool.returnConnection(obj);

    // then
    Assert.assertEquals(obj, "Hello");
    verify(objectPool).returnConnection(eq("Hello"));
  }

  public interface BlockingPool<T> {
    Optional<T> borrowConnection(long wait, TimeUnit unit);
    void returnConnection(T connection);

    boolean isClosed();
  }

  /**
   * Allows you to control the wait period and know if the connection is given or not, but
   * you still have to return by hand.
   */
  public void blockingInterfaceTest() {
    // given
    BlockingPool<String> objectPool = mock(BlockingPool.class);

    // when
    when(objectPool.borrowConnection(5, TimeUnit.SECONDS)).thenReturn(Optional.<String>absent());

    // then
    Optional<String> obj = objectPool.borrowConnection(5, TimeUnit.SECONDS);
    Assert.assertFalse(obj.isPresent());
  }

  public interface ExecutionPool<T> {
    <V> Future<V> submit(Task<T, V> task);

    public interface Task<T, V> {
      V call(T connection);
    }
  }

  /**
   * This interface looks too much like an executor, why not just use that interface?
   */
  public void executionPoolTest() {
    ExecutionPool<String> pool = mock(ExecutionPool.class);
    Future<String> result = pool.submit(new ExecutionPool.Task<String, String>() {
      @Override
      public String call(final String connection) {
        return connection;
      }
    });
  }

  public static abstract class ConntionCallable<T, V> implements Callable<V> {
    private final BlockingPool<T> pool;

    public ConntionCallable(final BlockingPool<T> pool) {
      Preconditions.checkArgument(! pool.isClosed(), "Pool is closed");
      this.pool = pool;
    }

    @Override
    public V call() throws Exception {
      Optional<T> optConnection = pool.borrowConnection(5, TimeUnit.SECONDS);
      if(optConnection.isPresent()) {
        T connection = optConnection.get();
        try {
          return withConnection(connection);
        } finally {
          pool.returnConnection(connection);
        }
      } else {
        throw new Exception("Unable to get connection");
      }
    }

    abstract V withConnection(T connection) throws Exception;
  }

  /**
   * This is a little better but what about retry?
   */
  public void executorWithConnectionPool() {
    ExecutorService executorService = mock(ExecutorService.class);
    final BlockingPool<String> pool = mock(BlockingPool.class);

    Future<String> result = executorService.submit(new ConntionCallable<String, String>(pool) {
      @Override String withConnection(final String connection) throws Exception {
        return connection;
      }
    });
  }

  public interface Retryer {
    <T> T callWithRetry(Callable<T> retryableTask) throws Exception;
  }

  /**
   * Create, now I can do retry with the same class!  but what if the pool could
   * execute for me?
   */
  public void retrableTask() throws Exception {
    Retryer retryer = mock(Retryer.class);
    final BlockingPool<String> pool = mock(BlockingPool.class);

    String result = retryer.callWithRetry(new ConntionCallable<String,String>(pool) {
      @Override String withConnection(final String connection) throws Exception {
        return connection;
      }
    });
  }

  public interface BlockingPoolWithCallable<T> {
    Optional<T> borrowConnection(long wait, TimeUnit unit);
    void returnConnection(T connection);

    boolean isClosed();

    <V> V execute(Function<T, V> function);
    <V> V executeWithRetry(Function<T, V> function, Retryer retryer);
  }

  /**
   * This is nice because there is less for you to work with (will replace function to make things clearer).
   *
   * The one issue with this is that the pool is now responsible for execution.  What if I want async execution?
   */
  public void retrableWithConnectionCallback() {
    Retryer retryer = mock(Retryer.class);
    final BlockingPoolWithCallable<String> pool = mock(BlockingPoolWithCallable.class);

    String result = pool.execute(new Function<String, String>() {
      @Override
      public String apply(final String s) {
        return s;
      }
    });

    String secondResult = pool.executeWithRetry(new Function<String, String>() {
      @Override
      public String apply(final String s) {
        return s;
      }
    }, retryer);
  }

}

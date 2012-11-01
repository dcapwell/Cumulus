package com.ekaqu.cumulus.pool;

import com.ekaqu.cumulus.retry.RetryException;
import com.ekaqu.cumulus.retry.Retryers;
import com.ekaqu.cumulus.util.Block;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Test(groups = "Experiment")
public class PoolExperiment {
  private static final Logger LOGGER = LoggerFactory.getLogger(PoolExperiment.class.getName());

  private final ExecutorService executorService = MoreExecutors.sameThreadExecutor();

  public interface Pool<T> {
    T get();

    void returnConnection(T connection);
  }

  /**
   * Doesn't support controling backoff time, so get should always be O(1) or could be an issue
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
   * Allows you to control the backoff period and know if the connection is given or not, but
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
      Preconditions.checkArgument(!pool.isClosed(), "Pool is closed");
      this.pool = pool;
    }

    @Override
    public V call() throws Exception {
      Optional<T> optConnection = pool.borrowConnection(5, TimeUnit.SECONDS);
      if (optConnection.isPresent()) {
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
      @Override
      String withConnection(final String connection) throws Exception {
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

    String result = retryer.callWithRetry(new ConntionCallable<String, String>(pool) {
      @Override
      String withConnection(final String connection) throws Exception {
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
   * <p/>
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

  public interface HealthChecker<T> {

    boolean isHealthy(T obj);

    void free(T obj);
  }

  // too step lookup.  Kinda ugly useage.  What about if the HC only had one method?
  public void connectionHealthCheck() {
    HealthChecker<String> stringHealthChecker = mock(HealthChecker.class);

    String string = "goodString";
    if (stringHealthChecker.isHealthy(string)) {
      // safe to use
    } else {
      stringHealthChecker.free(string);
    }
  }

  public interface HeathCheckv2<T> {
    boolean isBadThenFree(T obj);
  }

  /**
   * Making it one method saves typing but just makes it harder to understand.
   * <p/>
   * Also it doesn't let me know the state.  Is the pool still good to use?  Should I just free the object
   * or should i kill the pool?
   */
  public void connectionHealthCheckv2() {
    HeathCheckv2<String> stringHeathCheckv2 = mock(HeathCheckv2.class);

    String ref = "badString";
    if (stringHeathCheckv2.isBadThenFree(ref)) {
      // this is confusing, what do we do here?
    }
  }

  public interface ObjectFactory<T> extends Supplier<T> {

    boolean validate(T obj);

    void cleanup(T obj);
  }

  /**
   * Same as the first HealthChecker but merged the supplier.  Supplier was responsible for creating objects
   * so it should be responsible for knowing when objects are not valid and how to clean up.
   */
  public void evictIfNeeded() {
    ObjectFactory<String> stringObjectFactory = mock(ObjectFactory.class);
    String ref = stringObjectFactory.get();

    if (!stringObjectFactory.validate(ref)) {
      stringObjectFactory.cleanup(ref);
    }
  }

  /**
   * The ObjectManager is responsible for creating and destroying objects.
   */
  public interface ObjectManager<T> extends Supplier<T> {
    enum State {
      /**
       * The given object is in a good enough state to be regiven back to an Pool
       */
      VALID,
      /**
       * The given object is not in a good state and should be cleaned up
       */
      INVALID,
      /**
       * The pool can no longer be in a good state and should be invalidated
       */
      CLOSE_POOL
    }

    State validate(T obj, Optional<? extends Throwable> error);

    void cleanup(T obj);
  }

  public interface PoolWithErrors<T> extends Pool<T> {
    void returnToPoolWithException(T obj, Throwable throwable);
  }


  public void manager() {
    PoolWithErrors<Closeable> pool = mock(PoolWithErrors.class);
    Closeable closeable = pool.get();
    try {
      // some logic
      Closeables.close(closeable, true);
      // some logic

      pool.returnConnection(closeable);
    } catch (IOException e) {
      pool.returnToPoolWithException(closeable, e);
      // this could be error prone since there are two methods with similar names/params
    }
  }

  public void executingPool() {
    // given
    com.ekaqu.cumulus.pool.ObjectFactory<String> factory = mock(com.ekaqu.cumulus.pool.ObjectFactory.class);
    com.ekaqu.cumulus.pool.Pool<String> pool = new ObjectPool<String>(factory, executorService, 2, 2);

    ExecutingPool<String> executingPool = ExecutingPool.executor(pool);

    // when
    when(factory.get()).thenReturn("a", "b");
    when(factory.validate("a", null)).thenReturn(com.ekaqu.cumulus.pool.ObjectFactory.State.VALID);
    when(factory.validate("b", null)).thenReturn(com.ekaqu.cumulus.pool.ObjectFactory.State.VALID);
    executingPool.startAndWait();

    // then
    for (int i = 0; i < 4; i++) {
      executingPool.execute(new Block<String>() {
        @Override
        public void apply(final String operand) {
          LOGGER.info("Operand given {}", operand);
        }
      });
    }
  }

  @Test(expectedExceptions = RetryException.class)
  public void retryingExecutingPool() {
    // given
    com.ekaqu.cumulus.pool.ObjectFactory<String> factory = mock(AbstractObjectFactory.class, CALLS_REAL_METHODS);
    com.ekaqu.cumulus.pool.Pool<String> pool = new ObjectPool<String>(factory, executorService, 2, 2);
    com.ekaqu.cumulus.retry.Retryer retryer = Retryers.newRetryer(2);

    ExecutingPool<String> executingPool = ExecutingPool.retryingExecutor(pool, retryer);

    // when
    doReturn("a").when(factory).get();
    executingPool.startAndWait();

    // then
    for (int i = 0; i < 4; i++) {
      executingPool.execute(new Block<String>() {
        @Override
        public void apply(final String operand) {
          throw new RuntimeException("Operand " + operand);
        }
      });
    }
  }

}

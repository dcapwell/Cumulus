package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.retry.Retryer;
import com.ekaqu.cunulus.retry.Retryers;
import com.ekaqu.cunulus.util.Block;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = "Unit")
public class ExecutingPoolTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutingPoolTest.class.getName());

  private Pool<String> pool;
  private final Retryer retryer = Retryers.newRetryer(3);

  @BeforeClass(alwaysRun = true)
  public void before() {
    ObjectFactory<String> factory = mock(AbstractObjectFactory.class, CALLS_REAL_METHODS);
    doReturn("a").when(factory).get();
    when(factory.get()).thenReturn("a", "b", "c", "d");

    pool = new ObjectPool<String>(factory, 2, 4, MoreExecutors.sameThreadExecutor());
    pool.startAndWait();
  }

  public void basicExecutor() {
    // given
    ExecutingPool<String> executingPool = ExecutingPool.executor(pool);
    LOGGER.info("Pool {}", pool);

    // when
    boolean ran = executingPool.execute(new Block<String>() {
      @Override
      public void apply(final String s) {
        LOGGER.info("Given obj {}", s);
      }
    });

    // then
    Assert.assertTrue(ran, "Block not called");
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Block Error")
  public void basicExecutorWithException() {
    // given
    ExecutingPool<String> executingPool = ExecutingPool.executor(pool);
    LOGGER.info("Pool {}", pool);

    // when
    boolean ran = executingPool.execute(new Block<String>() {
      @Override
      public void apply(final String s) {
        LOGGER.info("Given obj {}", s);
        throw new RuntimeException("Block Error");
      }
    });

    // then
    Assert.fail("Unreachable");
  }

  public void basicExecutorBlocked() {
    // given
    ExecutingPool<String> executingPool = ExecutingPool.executor(pool);
    LOGGER.info("Pool {}", pool);

    // when
    boolean ran = executingPool.execute(new Block<String>() {
      @Override
      public void apply(final String s) {
        LOGGER.info("Given obj {}", s);
      }
    }, 5, TimeUnit.SECONDS);

    // then
    Assert.assertTrue(ran, "Block not called");
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Block Error")
  public void basicExecutorWithExceptionBlocked() {
    // given
    ExecutingPool<String> executingPool = ExecutingPool.executor(pool);
    LOGGER.info("Pool {}", pool);

    // when
    boolean ran = executingPool.execute(new Block<String>() {
      @Override
      public void apply(final String s) {
        LOGGER.info("Given obj {}", s);
        throw new RuntimeException("Block Error");
      }
    }, 5, TimeUnit.SECONDS);

    // then
    Assert.fail("Unreachable");
  }




  public void retryExecutor() {
    // given
    ExecutingPool<String> executingPool = ExecutingPool.retryingExecutor(pool, retryer);
    LOGGER.info("Pool {}", pool);

    // when
    boolean ran = executingPool.execute(new Block<String>() {
      @Override
      public void apply(final String s) {
        LOGGER.info("Given obj {}", s);
      }
    });

    // then
    Assert.assertTrue(ran, "Block not called");
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Block Error")
  public void retryExecutorWithException() {
    // given
    ExecutingPool<String> executingPool = ExecutingPool.retryingExecutor(pool, retryer);
    LOGGER.info("Pool {}", pool);

    // when
    boolean ran = executingPool.execute(new Block<String>() {
      @Override
      public void apply(final String s) {
        LOGGER.info("Given obj {}", s);
        throw new RuntimeException("Block Error");
      }
    });

    // then
    Assert.fail("Unreachable");
  }

  public void retryExecutorBlocked() {
    // given
    ExecutingPool<String> executingPool = ExecutingPool.retryingExecutor(pool, retryer);
    LOGGER.info("Pool {}", pool);

    // when
    boolean ran = executingPool.execute(new Block<String>() {
      @Override
      public void apply(final String s) {
        LOGGER.info("Given obj {}", s);
      }
    }, 5, TimeUnit.SECONDS);

    // then
    Assert.assertTrue(ran, "Block not called");
  }

  @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Block Error")
  public void retryExecutorWithExceptionBlocked() {
    // given
    ExecutingPool<String> executingPool = ExecutingPool.retryingExecutor(pool, retryer);
    LOGGER.info("Pool {}", pool);

    // when
    boolean ran = executingPool.execute(new Block<String>() {
      @Override
      public void apply(final String s) {
        LOGGER.info("Given obj {}", s);
        throw new RuntimeException("Block Error");
      }
    }, 5, TimeUnit.SECONDS);

    // then
    Assert.fail("Unreachable");
  }
}

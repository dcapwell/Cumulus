package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.pool.mocks.StringObjectFactory;
import com.ekaqu.cunulus.retry.BackOffPolicy;
import com.ekaqu.cunulus.retry.NoBackoffPolicy;
import com.ekaqu.cunulus.retry.RandomBackOffPolicy;
import com.ekaqu.cunulus.retry.Retryer;
import com.ekaqu.cunulus.retry.Retryers;
import com.ekaqu.cunulus.util.Block;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = "Unit")
public class ExecutingPoolTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutingPoolTest.class.getName());

  private Pool<String> pool;
  private final Retryer retryer = Retryers.newRetryer(3);

  private final StringObjectFactory stringFactory = new StringObjectFactory();
  private final ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).build();
  private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2 + 1;

  @BeforeClass(alwaysRun = true)
  public void before() {
    ObjectFactory<String> factory = mock(AbstractObjectFactory.class, CALLS_REAL_METHODS);
    doReturn("a").when(factory).get();
    when(factory.get()).thenReturn("a", "b", "c", "d");

    pool = new ObjectPool<String>(factory, MoreExecutors.sameThreadExecutor(), 2, 4);
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

  @Test(groups = "Experiment", description = "A pool without retries doesn't have a guaranty that a object is returned.  " +
      "This test is mostly to test timing and not a unit test")
  public void concurrentBlockExecute() throws InterruptedException {
    final ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_COUNT, threadFactory);

    final ExecutingPool<String> pool = new PoolBuilder<String>()
        .objectFactory(stringFactory).executorService(executorService)
        .corePoolSize(2).maxPoolSize(4).buildExecutingPool();

    final AtomicInteger counter = new AtomicInteger();
    final BackOffPolicy backOffPolicy = new RandomBackOffPolicy(500);
    final int iterations = 1000;
    for(int i = 0; i < iterations; i++) {
      final int finalI = i;
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          pool.execute(new Block<String>() {
            @Override
            public void apply(final String s) {
              LOGGER.info("Iteraction {}, data {}", finalI, s);
//              backOffPolicy.backoff(finalI);
              counter.incrementAndGet();
            }
          }, 5, TimeUnit.SECONDS);

        }
      });
    }

    executorService.shutdown();
    executorService.awaitTermination(50, TimeUnit.SECONDS);
//    TimeUnit.SECONDS.sleep(15);

    Assert.assertEquals(counter.get(), iterations);
  }

  public void concurrentBlockRetryExecute() throws InterruptedException {
    final ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_COUNT, threadFactory);

    final ExecutingPool<String> pool = new PoolBuilder<String>()
        .objectFactory(stringFactory).executorService(executorService)
        .corePoolSize(2).maxPoolSize(4).buildExecutingPool(
            // 10 should be enough so everyone gets an object
            // this will cause the last few executions to be slow since they have longer sleeps
            Retryers.newExponentialBackoffRetryer(10));

    final AtomicInteger counter = new AtomicInteger();
    final BackOffPolicy backOffPolicy = new RandomBackOffPolicy(500);
//    final BackOffPolicy backOffPolicy = new NoBackoffPolicy();
    final int iterations = 1000;
    for(int i = 0; i < iterations; i++) {
      final int finalI = i;
      executorService.submit(new Runnable() {
        @Override
        public void run() {
          pool.execute(new Block<String>() {
            @Override
            public void apply(final String s) {
              LOGGER.info("Iteraction {}, data {}", finalI, s);
              backOffPolicy.backoff(finalI);
              counter.incrementAndGet();
            }
          }, 5, TimeUnit.SECONDS);

        }
      });
    }

    executorService.shutdown();
    executorService.awaitTermination(50, TimeUnit.MINUTES);

    Assert.assertEquals(counter.get(), iterations);
  }
}

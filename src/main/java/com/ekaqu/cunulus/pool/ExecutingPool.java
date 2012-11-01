package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.retry.Retryer;
import com.ekaqu.cunulus.util.Block;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Decorates a pool and enables functional style access to the contents of a pool.
 * <p/>
 * When working with a pool, the normal code flow is as follows:
 * <pre>
 * {@code
 * final Optional<T> opt = pool.borrow();
 * if(opt.isPresent()) {
 *  final T obj = opt.get();
 *  try {
 *    LOGGER.info("Object given {}", obj);
 *    pool.returnToPool(obj);
 *  } catch (Throwable t) {
 *    pool.returnToPool(obj, t);
 *  }
 * }
 * }
 * </pre>
 * With the {@link ExecutingPool} you can just pass in a function that will work with the pooled object and let {@link
 * ExecutingPool} handle returning the object to the pool.
 * <p/>
 * Example:
 * <pre>
 * {@code
 * executingPool.execute(new Block<T>() {
 *    public void apply(final T obj) {
 *      LOGGER.info("Object given {}", obj);
 *    }
 * });
 * }
 * </pre>
 * <p/>
 * When exceptions are thrown, they will be propagated up as a {@link RuntimeException}.  If the exception is of type
 * {@link Error} or {@link RuntimeException} then it is rethrown
 */
public abstract class ExecutingPool<T> extends ForwardingPool<T> {

  /**
   * Creates a new executing pool around the given pool.
   *
   * @param pool to decorate
   */
  public ExecutingPool(final Pool<T> pool) {
    super(pool);
  }

  /**
   * Execute the given block passing in a value from the pool.
   *
   * @param block of code to execute
   * @return if pool had an element and that element was given to the block
   */
  public abstract boolean execute(Block<T> block);

  /**
   * Execute the given block passing in a value from the pool.
   *
   * @param block of code to execute
   * @param waitTime time to wait for a pooled object to show up.
   * @param unit     time unit used to determine how long to wait for an object to show up.
   * @return if pool had an element and that element was given to the block
   */
  public abstract boolean execute(Block<T> block, long waitTime, TimeUnit unit);

  /**
   * Creates a new {@link ExecutingPool}.  This executing pool will call the execute block in the same thread and at
   * most one time.
   *
   * @param pool to decorate
   * @param <T> type of pool
   * @return executing pool decorating the given pool
   */
  public static <T> ExecutingPool<T> executor(final Pool<T> pool) {
    return new ExecutingPool<T>(pool) {
      @Override
      public boolean execute(final Block<T> block) {
        final Optional<T> opt = pool.borrow();
        if (opt.isPresent()) {
          final T obj = opt.get();
          try {
            block.apply(obj);
            pool.returnToPool(obj);
          } catch (Throwable t) {
            pool.returnToPool(obj, t);
            throw Throwables.propagate(t);
          }
          return true;
        } else {
          return false;
        }
      }

      @Override
      public boolean execute(final Block<T> block, final long waitTime, final TimeUnit unit) {
        final Optional<T> opt = pool.borrow(waitTime, unit);
        if (opt.isPresent()) {
          final T obj = opt.get();
          try {
            block.apply(obj);
            pool.returnToPool(obj);
          } catch (Throwable t) {
            pool.returnToPool(obj, t);
            throw Throwables.propagate(t);
          }
          return true;
        } else {
          return false;
        }
      }
    };
  }

  /**
   * Creates a new {@link ExecutingPool} with retries.  This executing pool will call the execute block in the same
   * thread and potentially multiple times; based off the {@link Retryer}.
   *
   * @param pool to decorate
   * @param retryer used to retry operations
   * @param <T> type of pool
   * @return executing pool decorating the given pool
   */
  public static <T> ExecutingPool<T> retryingExecutor(final Pool<T> pool, final Retryer retryer) {
    return new ExecutingPool<T>(pool) {
      @Override
      public boolean execute(final Block<T> block) {
        try {
          return retryer.submitWithRetry(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
              final Optional<T> opt = pool.borrow();
              if (opt.isPresent()) {
                final T obj = opt.get();
                try {
                  block.apply(obj);
                  pool.returnToPool(obj);
                } catch (Throwable t) {
                  pool.returnToPool(obj, t);
                  throw Throwables.propagate(t);
                }
                return Boolean.TRUE;
              } else {
//                return Boolean.FALSE;
                // unable to get a pooled object, force retry.
                throw new ForcePoolRetryException();
              }
            }
          });
        } catch (ForcePoolRetryException e) {
          return Boolean.FALSE;
        } catch (Exception e) {
          // the callable doesn't throw an exception so this should be rare
          throw Throwables.propagate(e);
        }
      }

      @Override
      public boolean execute(final Block<T> block, final long waitTime, final TimeUnit unit) {
        try {
          return retryer.submitWithRetry(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
              final Optional<T> opt = pool.borrow(waitTime, unit);
              if (opt.isPresent()) {
                final T obj = opt.get();
                try {
                  block.apply(obj);
                  pool.returnToPool(obj);
                } catch (Throwable t) {
                  pool.returnToPool(obj, t);
                  throw Throwables.propagate(t);
                }
                return Boolean.TRUE;
              } else {
//                return Boolean.FALSE;
                // unable to get a pooled object, force retry.
                throw new ForcePoolRetryException();
              }
            }
          });
        } catch (ForcePoolRetryException e) {
          return Boolean.FALSE;
        } catch (Exception e) {
          // the callable doesn't throw an exception so this should be rare
          throw Throwables.propagate(e);
        }
      }

      /**
       * Used to force retry.
       */
      final class ForcePoolRetryException extends PoolRuntimeException {

        /**
         * Creates a new force pool retry exception.
         */
        public ForcePoolRetryException() {
          super("Force pool retry");
        }
      }
    };
  }
}

package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.retry.Retryer;
import com.ekaqu.cunulus.util.Block;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper around a pool that enables a more functional style access to the contents of a pool.
 *
 * When working with a pool, the normal code flow is as follows: {@code
final Optional<T> opt = pool.borrow();
if(opt.isPresent()) {
  final T obj = opt.get();
  try {
    LOGGER.info("Object given {}", obj);
    pool.returnToPool(obj);
  } catch (Throwable t) {
    pool.returnToPool(obj, t);
  }
}
}
 * With the {@link ExecutingPool} you can just pass in a function that will work with the pooled object and let
 * {@link ExecutingPool} handle returning the object to the pool.
 *
 * Example: {@code
executingPool.execute(new Block<T>() {
  @Override
  public void apply(final T obj) {
    LOGGER.info("Object given {}", obj);
  }
});
 }
 *
 * When exceptions are thrown, they will be propagated up as a RuntimeException.  If the exception is of type {@link Error}
 * or {@link RuntimeException} then it is rethrown
 */
public abstract class ExecutingPool<T> extends ForwardingPool<T> {
  public ExecutingPool(final Pool<T> pool) {
    super(pool);
  }

  /**
   * Execute the given block passing in a valid from the pool.
   * @return if pool had an element and that element was given to the block
   */
  abstract boolean execute(Block<T> block);

  /**
   * Execute the given block passing in a valid from the pool.
   *
   * @param waitTime time to wait for a pooled object to show up.
   * @param unit time unit used to determine how long to wait for an object to show up.
   * @return if pool had an element and that element was given to the block
   */
  abstract boolean execute(Block<T> block, long waitTime, TimeUnit unit);

  /**
   * Creates a new {@link ExecutingPool}.  {@link ExecutingPool#execute(com.ekaqu.cunulus.util.Block)} will call
   * the block at most one time.
   */
  public static <T> ExecutingPool<T> executor(final Pool<T> pool) {
    return new ExecutingPool<T>(pool) {
      @Override
      boolean execute(final Block<T> block) {
        final Optional<T> opt = pool.borrow();
        if(opt.isPresent()) {
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
      boolean execute(final Block<T> block, final long waitTime, final TimeUnit unit) {
        final Optional<T> opt = pool.borrow(waitTime, unit);
        if(opt.isPresent()) {
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
   * Creates a new {@link ExecutingPool} with retries.  {@link ExecutingPool#execute(com.ekaqu.cunulus.util.Block)} will
   * be called multiple times based off the {@link Retryer}.
   */
  public static <T> ExecutingPool<T> retryingExecutor(final Pool<T> pool, final Retryer retryer) {
    return new ExecutingPool<T>(pool) {
      @Override
      boolean execute(final Block<T> block) {
        try {
          return retryer.submitWithRetry(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
              final Optional<T> opt = pool.borrow();
              if(opt.isPresent()) {
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
                return Boolean.FALSE;
              }
            }
          });
        } catch (Exception e) {
          // the callable doesn't throw an exception so this should be rare
          throw Throwables.propagate(e);
        }
      }

      @Override
      boolean execute(final Block<T> block, final long waitTime, final TimeUnit unit) {
        try {
          return retryer.submitWithRetry(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
              final Optional<T> opt = pool.borrow(waitTime, unit);
              if(opt.isPresent()) {
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
                return Boolean.FALSE;
              }
            }
          });
        } catch (Exception e) {
          // the callable doesn't throw an exception so this should be rare
          throw Throwables.propagate(e);
        }
      }
    };
  }
}

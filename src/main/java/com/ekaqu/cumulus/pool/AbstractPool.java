package com.ekaqu.cumulus.pool;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base for building a new pool.
 *
 * @param <T> type of the pool
 */
@Beta
public abstract class AbstractPool<T> extends AbstractService implements Pool<T> {

  /**
   * Counts how many active elements are in the pool.  Active is defined as created.
   */
  private final AtomicInteger active = new AtomicInteger();

  /**
   * Min size of the pool.
   */
  private int corePoolSize = 0;

  /**
   * Max size of the pool.
   */
  private int maxPoolSize = 0;

  @Override
  protected final void doStart() {
    Preconditions.checkState(State.STARTING.equals(state()), "Not in the starting state: " + state());

    try {
      while (getActivePoolSize() < getCorePoolSize() && expand()) {
        // do nothing
      }
      notifyStarted();
    } catch (Exception e) {
      notifyFailed(e);
    }
  }

  @Override
  protected final void doStop() {
    Preconditions.checkState(State.STOPPING.equals(state()), "Not in the stopping state: " + state());

    try {
      // clean up pooled objects
      clear();

      notifyStopped();
    } catch (Exception e) {
      notifyFailed(e);
    }
  }

  /**
   * Same as {@link Pool#borrow(long, java.util.concurrent.TimeUnit)} with 0 and {@link TimeUnit#MILLISECONDS}.
   *
   * @see Pool#borrow(long, java.util.concurrent.TimeUnit)
   * @return optional element in the pool
   */
  @Override
  public final Optional<T> borrow() {
    checkNotClosed();

    return borrow(0, TimeUnit.MILLISECONDS);
  }

  /**
   * Same as {@link Pool#returnToPool(Object, Throwable)} with {@link Throwable} = null.
   *
   * @see Pool#returnToPool(Object, Throwable)
   * @param obj to add to the pool
   */
  @Override
  public final void returnToPool(final T obj) {
    Preconditions.checkNotNull(obj);

    checkNotClosed();

    returnToPool(obj, null);
  }

  /**
   * Uses the size method to determine if empty or not.
   *
   * @return if pool is empty
   */
  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public String toString() {
    return toStringBuilder().toString();
  }

  /**
   * Returns a ToStringHelper to help standardize toString format.
   *
   * @return ToStringHelper to use when overriding toString
   */
  protected final Objects.ToStringHelper toStringBuilder() {
    return Objects.toStringHelper(getClass())
        .add("state", state())
        .add("active", getActivePoolSize())
        .add("size", size())
        .add("corePoolSize", getCorePoolSize())
        .add("maxPoolSize", getMaxPoolSize());
  }

  @Override
  public int getCorePoolSize() {
    return corePoolSize;
  }

  @Override
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  @Override
  public int getActivePoolSize() {
    return active.get();
  }

  /**
   * Sets the core and max size for the pool.  Core size must be >= 0 and < max.
   * <p/>
   * This method is NOT thread safe.
   *
   * @param corePoolSize min size of the pool
   * @param maxPoolSize  max size of the pool
   */
  protected final void setPoolSizes(final int corePoolSize, final int maxPoolSize) {
    Preconditions.checkArgument(
        corePoolSize >= 0
            && maxPoolSize > 0
            && maxPoolSize >= corePoolSize
    );

    this.corePoolSize = corePoolSize;
    this.maxPoolSize = maxPoolSize;
  }

  /**
   * Attempts to expand the pool by one.
   *
   * @return if pool was expanded
   */
  protected final boolean expand() {
    boolean added = false;
    if (getActivePoolSize() < getMaxPoolSize()) {
      // there is room to expand, so lets create and add an object
      added = createAndAdd();
      if (added) {
        active.incrementAndGet();
      }
    }
    return added;
  }

  /**
   * Attempts to shrink the pool to corePoolSize.
   *
   * @return if pool shrunk.  If shrinking still left the pool larger than core, true should be returned
   */
  protected final boolean shrink() {
    final int shrinkBy = getActivePoolSize() - getCorePoolSize();
    // only shrink if active count is larger than core size
    int removed = 0;
    if (shrinkBy > 0) {
      removed = shrink(shrinkBy);
      active.addAndGet(0 - removed); // removed should always be 0 or positive
    }
    return removed > 0;
  }

  /**
   * Checks if the pool is closed, if so it throws a runtime exception.
   * <p/>
   * This method should only be for internal checks and not targeted for users
   *
   * @throws ClosedPoolException pool is closed
   */
  protected final  void checkNotClosed() {
    if (!isRunning()) {
      throw new ClosedPoolException();
    }
  }

  /**
   * Checks if the current pool is full.  If a pool is full then a minimum of {@link
   * com.ekaqu.cumulus.pool.AbstractPool#getCorePoolSize()} is available
   *
   * @return if full or not
   */
  protected final boolean isFull() {
    return size() >= getActivePoolSize();
  }

  /**
   * Create a new object and add it to the pool if pool size is not too large. This method shouldn't create new objects
   * if max size has not been surceased.
   *
   * @return true if added object to pool
   */
  protected abstract boolean createAndAdd();

  /**
   * Shrinks the current pool by shrinkBy.  If size is 10 and shrinkBy is 2, then the end result should be 8.
   *
   * @param shrinkBy how many objects should be removed
   * @return how many objects were removed.  Should always be 0 or more
   */
  protected abstract int shrink(final int shrinkBy);

  /**
   * Clear elements out of the pool.  All resources should be freed up.
   */
  protected abstract void clear();
}

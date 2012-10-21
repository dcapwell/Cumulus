package com.ekaqu.cunulus.pool;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Beta
public abstract class AbstractPool<T> extends AbstractService implements Pool<T> {

  private final AtomicInteger active = new AtomicInteger();

  private int corePoolSize = 0;
  private int maxPoolSize = 0;

  /**
   * @see com.google.common.util.concurrent.AbstractService#doStart()
   * {@inheritDoc}
   */
  @Override
  protected void doStart() {
    Preconditions.checkState(State.STARTING.equals(state()), "Not in the starting state: " + state());

    try {
      while (active.get() < corePoolSize && expand()) { }
      notifyStarted();
    } catch (Exception e) {
      notifyFailed(e);
    }
  }

  /**
   * @see com.google.common.util.concurrent.AbstractService#doStop()
   * {@inheritDoc}
   */
  @Override
  protected void doStop() {
    Preconditions.checkState(State.STOPPING.equals(state()), "Not in the stopping state: " + state());

    try{
      // clean up pooled objects
      clear();

      notifyStopped();
    } catch (Exception e) {
      notifyFailed(e);
    }
  }

  /**
   * Same as {@link Pool#borrow(long, java.util.concurrent.TimeUnit)} with 0 and {@link TimeUnit#MILLISECONDS}
   *
   * @see Pool#borrow(long, java.util.concurrent.TimeUnit)
   * {@inheritDoc}
   */
  @Override
  public Optional<T> borrow() {
    checkNotClosed();

    return borrow(0, TimeUnit.MILLISECONDS);
  }

  /**
   * Same as {@link Pool#returnToPool(Object, Throwable)} with {@link Throwable} = null
   *
   * @see Pool#returnToPool(Object, Throwable)
   * {@inheritDoc}
   */
  @Override
  public void returnToPool(final T obj) {
    Preconditions.checkNotNull(obj);

    checkNotClosed();

    returnToPool(obj, null);
  }

  /**
   * Uses the size method to determine if empty or not
   */
  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("{state=").append(state());
    sb.append(", active=").append(getActiveCount());
    sb.append(", size=").append(size());
    sb.append(", corePoolSize=").append(getCorePoolSize());
    sb.append(", maxPoolSize=").append(getMaxPoolSize());
    sb.append('}');
    return sb.toString();
  }

  public int getCorePoolSize() {
    return corePoolSize;
  }

  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  public int getActiveCount() {
    return active.get();
  }

  protected void setPoolSizes(final int corePoolSize, final int maxPoolSize) {
    Preconditions.checkArgument(
        corePoolSize >= 0
            && maxPoolSize > 0
            && maxPoolSize >= corePoolSize
    );

    this.corePoolSize = corePoolSize;
    this.maxPoolSize = maxPoolSize;
  }

  protected boolean expand() {
    boolean added = false;
    if(active.get() < getMaxPoolSize()) {
      // there is room to expand, so lets create and add an object
      added = createAndAdd();
      if(added) active.incrementAndGet();
    }
    return added;
  }

  protected boolean shrink() {
    boolean shrunk = false;
    if(active.get() > getCorePoolSize()) {
      // only shrink if active count is larger than core size
      int count = shrink(active.get() - getCorePoolSize());
      if(count > 0) {
        active.addAndGet(count * -1);
        shrunk = true;
      }
    }
    return shrunk;
  }

  /**
   * Checks if the pool is closed, if so it throws a runtime exception
   *
   * @throws ClosedPoolException pool is closed
   */
  protected void checkNotClosed() {
    if (!isRunning()) {
      throw new ClosedPoolException();
    }
  }

  /**
   * Checks if the current pool is full.  If a pool is full then a minimum of {@link com.ekaqu.cunulus.pool.AbstractPool#getCorePoolSize()} is available
   * @return if full or not
   */
  protected boolean isFull() {
    return size() >= active.get();
  }

  /**
   * Create a new object and add it to the pool if pool size is not too large. This method shouldn't create
   * new objects if max size has not been surceased.
   * @return true if added object to pool
   */
  protected abstract boolean createAndAdd();

  /**
   * Shrinks the current pool by shrinkBy.  If size is 10 and shrinkBy is 2, then the end result should be 8.
   * @param shrinkBy how many objects should be removed
   * @return how many objects were removed.  Should always be 0 or more
   */
  protected abstract int shrink(final int shrinkBy);

  /**
   * Clear elements out of the pool.  All resources should be freed up.
   */
  protected abstract void clear();
}

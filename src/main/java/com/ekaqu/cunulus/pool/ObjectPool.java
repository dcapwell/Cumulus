package com.ekaqu.cunulus.pool;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

//TODO add shrinking
//TODO add JMX
public class ObjectPool<T> extends AbstractPool<T> {

  private final BlockingQueue<T> available = Queues.newLinkedBlockingQueue();

  private final ObjectFactory<T> objectFactory;
  private final ExecutorService executorService;

  private Runnable createObjectRunnable = new Runnable() {
    @Override
    public void run() {
      expand();
    }
  };

  public ObjectPool(final ObjectFactory<T> objectFactory, final ExecutorService executorService,
                    final int corePoolSize, final int maxPoolSize) {
    this.objectFactory = objectFactory;
    this.executorService = executorService;

    setPoolSizes(corePoolSize, maxPoolSize);
  }

  @Override
  public Optional<T> borrow(final long timeout, final TimeUnit unit) {
    checkNotClosed();

    // should be non blocking, just get the head and return that
    T obj = this.available.poll();
    if (obj == null) {
      // pool is empty, see if a new object can be created
      tryCreateAsync();

      // backoff for a object to be added and return that
      try {
        obj = this.available.poll(timeout, unit);
      } catch (InterruptedException e) {
        // something interrupted the backoff, interrupt the current thread
        Thread.currentThread().interrupt();
      }
    }
    return Optional.fromNullable(obj);
  }

  @Override
  public void returnToPool(final T obj, final Throwable throwable) {
    Preconditions.checkNotNull(obj);

    checkNotClosed();

    // validate obj
    ObjectFactory.State state = objectFactory.validate(obj, throwable);
    switch (state) {
      case VALID:
        // just add back to the pool if pool can support it
        if (isFull()) {
          // clean up since pool has enough elements right now
          objectFactory.cleanup(obj);
        } else {
          available.offer(obj);
        }
        break;
      case INVALID:
        // clean up object
        objectFactory.cleanup(obj);
        break;
      case CLOSE_POOL:
        objectFactory.cleanup(obj);
        stopAndWait();
        break;
      default:
        throw new UnsupportedOperationException("Unknown state " + state);
    }

    // if pool size has changed, then attempt to shrink
    if(getActiveCount() > getMaxPoolSize()) {
      shrink();
    }
  }

  @Override
  public int size() {
    return this.available.size();
  }

  @Override
  protected boolean createAndAdd() {
    boolean added = false;
    T obj = objectFactory.get();
    if (available.offer(obj)) {
      added = true;
    } else {
      objectFactory.cleanup(obj);
    }
    return added;
  }

  @Override
  protected int shrink(final int shrinkBy) {
    List<T> objects = Lists.newArrayList();
    available.drainTo(objects, shrinkBy);

    int numObjects = objects.size();
    for(final T obj : objects) {
      objectFactory.cleanup(obj);
    }
    return numObjects;
  }

  @Override
  protected void clear() {
    List<T> objs = Lists.newArrayList();
    available.drainTo(objs);
    for (T obj : objs) {
      objectFactory.cleanup(obj);
    }
  }

  /**
   * calls {@link com.ekaqu.cunulus.pool.ObjectPool#expand()} in the background
   */
  private void tryCreateAsync() {
    Future<?> result = this.executorService.submit(createObjectRunnable);
  }
}

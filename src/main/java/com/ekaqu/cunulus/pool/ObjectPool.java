package com.ekaqu.cunulus.pool;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.AbstractService;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//TODO implement monitoring the pool to know when to shrink it
//TODO add JMX
public class ObjectPool<T> extends AbstractService implements Pool<T> {
  private final int maximumPoolSize, corePoolSize;
  private final AtomicInteger availableCount = new AtomicInteger();
  private final ObjectFactory<T> objectFactory;
  private final BlockingQueue<T> available;
  private final ExecutorService executorService;
  private Runnable createObjectRunnable = new Runnable() {
    @Override
    public void run() {
      createAndAdd();
    }
  };

  public ObjectPool(final ObjectFactory<T> objectFactory,
                    final int corePoolSize, final int maximumPoolSize,
                    final ExecutorService executorService) {
    Preconditions.checkArgument(
        corePoolSize >= 0
            && maximumPoolSize > 0
            && maximumPoolSize >= corePoolSize
    );

    this.objectFactory = Preconditions.checkNotNull(objectFactory);

    this.maximumPoolSize = maximumPoolSize;
    this.corePoolSize = corePoolSize;

    this.executorService = Preconditions.checkNotNull(executorService);
    this.available = Queues.newLinkedBlockingQueue(maximumPoolSize);
  }

  @Override
  protected void doStart() {
    Preconditions.checkState(State.STARTING.equals(state()), "Not in the starting state: " + state());
    try {
      while (availableCount.get() < corePoolSize && createAndAdd()) { }
      notifyStarted();
    } catch (Exception e) {
      notifyFailed(e);
    }
  }

  @Override
  protected void doStop() {
    Preconditions.checkState(State.STOPPING.equals(state()), "Not in the stopping state: " + state());

    // clean up pooled objects
    List<T> objs = Lists.newArrayList();
    available.drainTo(objs);
    for(T obj : objs) {
      objectFactory.cleanup(obj);
    }

    notifyStopped();
  }

  @Override
  public Optional<T> borrow() {
    checkNotClosed();

    return Optional.fromNullable(this.available.poll());
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
  public void returnToPool(final T obj) {
    returnToPool(obj, null);
  }

  @Override
  public void returnToPool(T obj, Throwable throwable) {
    Preconditions.checkNotNull(obj);

    checkNotClosed();

    // validate obj
    ObjectFactory.State state = objectFactory.validate(obj, throwable);
    switch (state) {
      case VALID:
        // just add back to the pool
        available.offer(obj);
        break;
      case INVALID:
        // clean up object
        objectFactory.cleanup(obj);
        break;
      case CLOSE_POOL:
        stopAndWait();
        break;
      default:
        throw new UnsupportedOperationException("Unknown state " + state);
    }
  }

  @Override
  public int size() {
    return this.available.size();
  }

  @Override
  public boolean isEmpty() {
    return this.available.isEmpty();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ObjectPool");
    sb.append("{maximumPoolSize=").append(maximumPoolSize);
    sb.append(", corePoolSize=").append(corePoolSize);
    sb.append(", availableCount=").append(availableCount);
    sb.append(", available=").append(available);
    sb.append('}');
    return sb.toString();
  }

  /**
   * If the pool is not full, create a new object and add it to the pool
   * @return object was added or not
   */
  private boolean createAndAdd() {
    boolean added = false;
    //TODO should this lock? There is a chance that the number of objects is larger than max size
    //TODO queue is capped so going larger shouldn't be an issue but count might get larger
    if(maximumPoolSize > availableCount.get()) {
      T obj = objectFactory.get();
      if(available.offer(obj)) {
        availableCount.incrementAndGet();
        added = true;
      } else {
        throw new IllegalStateException("Unable to add object to pool");
      }
    }
    return added;
  }

  /**
   * Checks if the pool is closed, if so it throws a runtime exception
   *
   * @throws ClosedPoolException pool is closed
   */
  private void checkNotClosed() {
    if (!isRunning()) {
      throw new ClosedPoolException();
    }
  }

  /**
   * Attempts to create a new object in the background if the pool is allowed to expand
   */
  private void tryCreateAsync() {
    if(maximumPoolSize > availableCount.get()) {
      Future<?> result = this.executorService.submit(createObjectRunnable);
    }
  }
}

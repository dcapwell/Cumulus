package com.ekaqu.cunulus.pool;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @TODO Add monitoring
 * @TODO when idle count > corePoolSize for long enough, start removing objects
 */
@Beta
public class SimplePool<T> extends AbstractService implements Pool<T> {

  private final int maximumPoolSize, corePoolSize;
  private final AtomicInteger availableCount = new AtomicInteger();
  private final Supplier<T> supplier;
  private final BlockingQueue<T> available;
  private final ListeningExecutorService executorService;
  private final FutureCallback<T> createObjectListener = new FutureCallback<T>() {
    @Override
    public void onSuccess(final T result) {
      checkNotClosed();

      if(available.offer(result)) {
        availableCount.getAndIncrement();
      }
    }

    @Override
    public void onFailure(final Throwable t) {
      //TODO handle this.  Close the pool?
    }
  };
  private final Callable<T> createObjectCallable = new Callable<T>() {
    @Override
    public T call() throws Exception {
      T obj = supplier.get();
      Preconditions.checkNotNull(obj, "Not allowed to put null in the pool");
      return obj;
    }
  };

  public SimplePool(final Supplier<T> supplier, final ListeningExecutorService executorService,
      final int corePoolSize, final int maximumPoolSize) {
    Preconditions.checkArgument(
        corePoolSize >= 0
        && maximumPoolSize > 0
        && maximumPoolSize >= corePoolSize
    );

    this.maximumPoolSize = maximumPoolSize;
    this.corePoolSize = corePoolSize;

    this.supplier = Preconditions.checkNotNull(supplier);
    this.executorService = Preconditions.checkNotNull(executorService);
    this.available = Queues.newLinkedBlockingQueue(maximumPoolSize);

    MoreExecutors.addDelayedShutdownHook(executorService, 500, TimeUnit.MILLISECONDS); //TODO magic number...
  }

  @Override
  protected void doStart() {
    Preconditions.checkState(State.STARTING.equals(state()), "Not in the starting state: " + state());
    try {
      for (int i = 0; i < corePoolSize; i++) {
        available.offer(createObjectCallable.call());
        availableCount.incrementAndGet();
      }
      notifyStarted();
    } catch (Exception e) {
      notifyFailed(e);
    }
  }

  @Override
  protected void doStop() {
    checkNotClosed();

    // close executor so nothing adds in the background
    this.executorService.shutdown();

    // clean up pool
    this.available.clear();

    notifyStopped();
  }

  @Override
  public Optional<T> borrow(final long timeout, final TimeUnit unit) {
    checkNotClosed();

    // should be non blocking, just get the head and return that
    T obj = this.available.poll();
    if (obj == null) {
      // pool is empty, see if a new connection can be created
      tryCreateAsync();

      // backoff for a connection to be added and return that
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
    Preconditions.checkNotNull(obj);

    checkNotClosed();

    available.offer(obj);
  }

  @Override
  public int size() {
    return this.available.size();
  }

  @Override
  public boolean isEmpty() {
    return this.available.isEmpty();
  }

  /**
   * Attempts to create a new object in the background if the pool is allowed to expand
   */
  private void tryCreateAsync() {
    if(maximumPoolSize > availableCount.get()) {
      ListenableFuture<T> result = this.executorService.submit(createObjectCallable);
      Futures.addCallback(result, createObjectListener);
    }
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
}

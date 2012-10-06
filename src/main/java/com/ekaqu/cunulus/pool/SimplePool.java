package com.ekaqu.cunulus.pool;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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

public class SimplePool<T> extends AbstractService implements Pool<T> {

  private final int maxObjects, initialObjects;
  private final AtomicInteger availableCount = new AtomicInteger();
  private final ObjectFactory<T> factory;
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
      T obj = factory.get();
      Preconditions.checkNotNull(obj, "Not allowed to put null in the pool");
      return obj;
    }
  };

  public SimplePool(final ObjectFactory<T> factory, final ListeningExecutorService executorService,
                    final int initialObjects, final int maxObjects) {
    Preconditions.checkArgument(maxObjects >= initialObjects, "Max Objects should be equal to or larger than initial objects");

    this.maxObjects = maxObjects;
    this.initialObjects = initialObjects;

    this.factory = Preconditions.checkNotNull(factory);
    this.executorService = Preconditions.checkNotNull(executorService);
    this.available = Queues.newLinkedBlockingQueue(maxObjects);

    MoreExecutors.addDelayedShutdownHook(executorService, 500, TimeUnit.MILLISECONDS); //TODO magic number...
  }

  @Override
  protected void doStart() {
    Preconditions.checkState(State.STARTING.equals(state()), "Not in the starting state: " + state());
    try {
      for (int i = 0; i < initialObjects; i++) {
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
    while (!this.available.isEmpty()) {
      T obj = available.poll();
      factory.cleanup(obj);
    }

    notifyStopped();
  }

  /**
   * {@inheritDoc Pool#borrow(long, java.util.concurrent.TimeUnit)}
   *
   * @throws ClosedPoolException pool is closed
   */
  @Override
  public Optional<T> borrow(final long timeout, final TimeUnit unit) {
    checkNotClosed();

    // should be non blocking, just get the head and return that
    T obj = this.available.poll();
    if (obj == null) {
      // pool is empty, see if a new connection can be created
      tryCreateAsync();

      // wait for a connection to be added and return that
      try {
        obj = this.available.poll(timeout, unit);
      } catch (InterruptedException e) {
        // something interrupted the wait, interrupt the current thread
        Thread.currentThread().interrupt();
      }
    }
    return Optional.fromNullable(obj);
  }

  /**
   * Attempts to create a new object in the background if the pool is allowed to expand
   */
  private void tryCreateAsync() {
    if(maxObjects > availableCount.get()) {
      ListenableFuture<T> result = this.executorService.submit(createObjectCallable);
      Futures.addCallback(result, createObjectListener);
    }
  }

  @Override
  public void returnToPool(final T obj) {
    checkNotClosed();

    if (factory.shouldAddToPool(obj)) {
      // safe to add obj to pool, but check to see if pool is too large already
      if(maxObjects < availableCount.get()) {
        factory.cleanup(obj);
      } else {
        available.offer(obj);
      }
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

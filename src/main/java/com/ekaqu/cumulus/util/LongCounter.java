package com.ekaqu.cumulus.util;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Basic counter class backed by a long.  Methods are the same as {@link java.util.concurrent.atomic.AtomicLong} so it
 * should be easy to swap between the two.
 * <p/>
 * This class is not thread safe but can still be used concurrently if only incrementing/decrementing and an approximate
 * number is fine.  For precise results in a concurrent environment use {@link java.util.concurrent.atomic.AtomicLong}
 * <p/>
 * If you can get away with using a primitive long, use that instead of this class since its faster.
 */
@NotThreadSafe
public final class LongCounter extends Number {

  /**
   * Counter used by all API calls.
   */
  private long counter;

  /**
   * Creates a new counter with the long as the starting value.
   *
   * @param i starting value
   */
  public LongCounter(final long i) {
    counter = i;
  }

  /**
   * Creates a new counter with 0 as the starting value.
   */
  public LongCounter() {
    counter = 0;
  }

  /**
   * Get the current value of the counter.
   *
   * @return current value
   */
  public long get() {
    return counter;
  }

  /**
   * Set a new value for the counter.
   *
   * @param i value to set the counter to
   */
  public void set(final long i) {
    counter = i;
  }

  /**
   * Eventually sets to the given value.
   * <p/>
   * This method is the same as {@link LongCounter#set(long)} but here to match {@link
   * java.util.concurrent.atomic.AtomicLong}
   *
   * @param i value to set the counter to
   */
  public void lazySet(final long i) {
    set(i);
  }

  /**
   * Set the value and return the old value.
   *
   * @param i value to set
   * @return old value
   */
  public long getAndSet(final long i) {
    long old = counter;
    counter = i;
    return old;
  }

  /**
   * Set the counter to update iff {@link com.ekaqu.cumulus.util.LongCounter#get()} == expect.  Returns if value was
   * accepted.
   *
   * @param expect what the current value should be
   * @param update value to set
   * @return if the value was accepted
   */
  public boolean compareAndSet(final long expect, final long update) {
    if (counter == expect) {
      counter = update;
      return true;
    }
    return false;
  }

  /**
   * Set the counter to update iff {@link com.ekaqu.cumulus.util.LongCounter#get()} == expect.  Returns if value was
   * accepted.
   * <p/>
   * This is the same as {@link LongCounter#compareAndSet(long, long)} but here to match {@link
   * java.util.concurrent.atomic.AtomicLong}
   *
   * @param expect what the current value should be
   * @param update value to set
   * @return if the value was accepted
   */
  public boolean weakCompareAndSet(final long expect, final long update) {
    return compareAndSet(expect, update);
  }

  /**
   * Increments by one the current value and returns the old value.
   *
   * @return value before this method was executed
   */
  public long getAndIncrement() {
    long old = counter;
    counter++;
    return old;
  }

  /**
   * Decrements by one the current value and returns the old value.
   *
   * @return value before this method was executed
   */
  public long getAndDecrement() {
    long old = counter;
    counter--;
    return old;
  }

  /**
   * Adds by i the current value and returns the old value.
   *
   * @param i value to add
   * @return value before this method was executed
   */
  public long getAndAdd(final long i) {
    long old = counter;
    counter += i;
    return old;
  }

  /**
   * Increments by one the current value; returns the result.
   *
   * @return incremented value
   */
  public long incrementAndGet() {
    return ++counter;
  }

  /**
   * Decrements by one the current value; returns the result.
   *
   * @return Decremented value
   */
  public long decrementAndGet() {
    return --counter;
  }

  /**
   * Adds by i the current value; returns the result.
   *
   * @param i added to counter
   * @return Added value
   */
  public long addAndGet(final long i) {
    return counter += i;
  }

  @Override
  public  String toString() {
    return Long.toString(counter);
  }

  @Override
  public int intValue() {
    return (int) counter;
  }

  @Override
  public long longValue() {
    return counter;
  }

  @Override
  public float floatValue() {
    return (float) counter;
  }

  @Override
  public double doubleValue() {
    return (double) counter;
  }
}

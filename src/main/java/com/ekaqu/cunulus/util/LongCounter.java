package com.ekaqu.cunulus.util;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Basic counter class backed by a long.  Methods are the same as {@link java.util.concurrent.atomic.AtomicLong} so it should be easy to swap between the two.
 * <p/>
 * This class is not thread safe but can still be used concurrently if only incrementing/decrementing and an
 * approximate number is fine.  For precise results in a concurrent environment use {@link java.util.concurrent.atomic.AtomicLong}
 * <p/>
 * If you can get away with using a primitive long, use that instead of this class since its faster.
 */
@NotThreadSafe
public class LongCounter extends Number {

  private long counter;

  public LongCounter(long i) {
    counter = i;
  }

  public LongCounter() {
    counter = 0;
  }

  /**
   * Get the current value of the counter
   */
  public final long get() {
    return counter;
  }

  /**
   * Set a new value for the counter
   */
  public final void set(long i) {
    counter = i;
  }

  /**
   * Eventually sets to the given value.
   * <p/>
   * This method is the same as {@link LongCounter#set(long)} but here to match {@link java.util.concurrent.atomic.AtomicLong}
   */
  public final void lazySet(long i) {
    set(i);
  }

  /**
   * Set the value and return the old value.
   *
   * @param i value to set
   * @return old value
   */
  public final long getAndSet(long i) {
    final long old = counter;
    counter = i;
    return old;
  }

  /**
   * Set the counter to update iff {@link com.ekaqu.cunulus.util.LongCounter#get()} == expect.  Returns if
   * value was accepted.
   *
   * @param expect what the current value should be
   * @param update value to set
   * @return if the value was accepted
   */
  public final boolean compareAndSet(long expect, long update) {
    if (counter == expect) {
      counter = update;
      return true;
    }
    return false;
  }

  /**
   * Set the counter to update iff {@link com.ekaqu.cunulus.util.LongCounter#get()} == expect.  Returns if
   * value was accepted.
   * <p/>
   * This is the same as {@link LongCounter#compareAndSet(long, long)} but here to match {@link java.util.concurrent.atomic.AtomicLong}
   *
   * @param expect what the current value should be
   * @param update value to set
   * @return if the value was accepted
   */
  public final boolean weakCompareAndSet(long expect, long update) {
    return compareAndSet(expect, update);
  }

  /**
   * Increments by one the current value and returns the old value
   *
   * @return value before this method was executed
   */
  public final long getAndIncrement() {
    final long old = counter;
    counter++;
    return old;
  }

  /**
   * Decrements by one the current value and returns the old value
   *
   * @return value before this method was executed
   */
  public final long getAndDecrement() {
    final long old = counter;
    counter--;
    return old;
  }

  /**
   * Adds by i the current value and returns the old value
   *
   * @param i value to add
   * @return value before this method was executed
   */
  public final long getAndAdd(long i) {
    final long old = counter;
    counter += i;
    return old;
  }

  /**
   * Increments by one the current value; returns the result.
   *
   * @return incremented value
   */
  public final long incrementAndGet() {
    return ++counter;
  }

  /**
   * Decrements by one the current value; returns the result.
   *
   * @return Decremented value
   */
  public final long decrementAndGet() {
    return --counter;
  }

  /**
   * Adds by i the current value; returns the result.
   *
   * @return Added value
   */
  public final long addAndGet(long i) {
    return counter += i;
  }

  @Override
  public String toString() {
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

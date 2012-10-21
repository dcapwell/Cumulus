package com.ekaqu.cunulus.util;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Basic counter class backed by a int.  Methods are the same as AtomicInteger so it should be easy to swap between the two.
 * <p/>
 * This class is not thread safe but can still be used concurrently if only incrementing/decrementing and an
 * approximate number is fine.  For precise results in a concurrent environment use {@link java.util.concurrent.atomic.AtomicInteger}.
 *
 * If you can get away with using a primitive int, use that instead of this class since its faster.
 */
@NotThreadSafe
public class IntegerCounter extends Number {

  private int counter;

  public IntegerCounter(int i) {
    counter = i;
  }

  public IntegerCounter() {
    counter = 0;
  }

  /**
   * Get the current value of the counter
   */
  public final int get() {
    return counter;
  }

  /**
   * Set a new value for the counter
   */
  public final void set(int i) {
    counter = i;
  }

  /**
   * Eventually sets to the given value.
   *
   * This method is the same as {@link IntegerCounter#set(int)} but here to match {@link java.util.concurrent.atomic.AtomicInteger}
   */
  public final void lazySet(int i) {
    set(i);
  }

  /**
   * Set the value and return the old value.
   * @param i value to set
   * @return old value
   */
  public final int getAndSet(int i) {
    final int old = counter;
    counter = i;
    return old;
  }

  /**
   * Set the counter to update iff {@link com.ekaqu.cunulus.util.IntegerCounter#get()} == expect.  Returns if
   * value was accepted.
   * @param expect what the current value should be
   * @param update value to set
   * @return if the value was accepted
   */
  public final boolean compareAndSet(int expect, int update) {
    if (counter == expect) {
      counter = update;
      return true;
    }
    return false;
  }

  /**
   * Set the counter to update iff {@link com.ekaqu.cunulus.util.IntegerCounter#get()} == expect.  Returns if
   * value was accepted.
   *
   * This is the same as {@link IntegerCounter#compareAndSet(int, int)} but here to match {@link java.util.concurrent.atomic.AtomicInteger}
   * @param expect what the current value should be
   * @param update value to set
   * @return if the value was accepted
   */
  public final boolean weakCompareAndSet(int expect, int update) {
    return compareAndSet(expect, update);
  }

  /**
   * Increments by one the current value and returns the old value
   * @return value before this method was executed
   */
  public final int getAndIncrement() {
    final int old = counter;
    counter++;
    return old;
  }

  /**
   * Decrements by one the current value and returns the old value
   * @return value before this method was executed
   */
  public final int getAndDecrement() {
    final int old = counter;
    counter--;
    return old;
  }

  /**
   * Adds by i the current value and returns the old value
   * @param i value to add
   * @return value before this method was executed
   */
  public final int getAndAdd(int i) {
    final int old = counter;
    counter += i;
    return old;
  }

  /**
   * Increments by one the current value; returns the result.
   * @return incremented value
   */
  public final int incrementAndGet() {
    return ++counter;
  }

  /**
   * Decrements by one the current value; returns the result.
   * @return Decremented value
   */
  public final int decrementAndGet() {
    return --counter;
  }

  /**
   * Adds by i the current value; returns the result.
   * @return Added value
   */
  public final int addAndGet(int i) {
    return counter += i;
  }

  @Override
  public String toString() {
    return Integer.toString(counter);
  }

  @Override
  public int intValue() {
    return counter;
  }

  @Override
  public long longValue() {
    return (long) counter;
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

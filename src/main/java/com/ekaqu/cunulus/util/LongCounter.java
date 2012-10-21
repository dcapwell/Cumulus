package com.ekaqu.cunulus.util;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Basic counter class backed by a long.  Methods are the same as AtomicLong so it should be easy to swap between the two.
 * <p/>
 * This class is not thread safe
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

  public final long get() {
    return counter;
  }

  public final void set(long i) {
    counter = i;
  }

  public final void lazySet(long i) {
    set(i);
  }

  public final long getAndSet(long i) {
    final long old = counter;
    counter = i;
    return old;
  }

  public final boolean compareAndSet(long expect, long update) {
    if (counter == expect) {
      counter = update;
      return true;
    }
    return false;
  }

  public final boolean weakCompareAndSet(long expect, long update) {
    return compareAndSet(expect, update);
  }

  public final long getAndIncrement() {
    final long old = counter;
    counter++;
    return old;
  }

  public final long getAndDecrement() {
    final long old = counter;
    counter--;
    return old;
  }

  public final long getAndAdd(long i) {
    final long old = counter;
    counter += i;
    return old;
  }

  public final long incrementAndGet() {
    return ++counter;
  }

  public final long decrementAndGet() {
    return --counter;
  }

  public final long addAndGet(long i) {
    return counter += i;
  }

  public String toString() {
    return Long.toString(counter);
  }

  public int intValue() {
    return (int) counter;
  }

  public long longValue() {
    return counter;
  }

  public float floatValue() {
    return (float) counter;
  }

  public double doubleValue() {
    return (double) counter;
  }
}

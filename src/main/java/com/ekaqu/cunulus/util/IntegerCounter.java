package com.ekaqu.cunulus.util;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Basic counter class backed by a int.  Methods are the same as AtomicInteger so it should be easy to swap between the two.
 * <p/>
 * This class is not thread safe but can still be used concurrently if only incrementing/decrementing and an
 * approximate number is fine.  For precise results in a concurrent environment use {@link java.util.concurrent.atomic.AtomicInteger}
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

  public final int get() {
    return counter;
  }

  public final void set(int i) {
    counter = i;
  }

  public final void lazySet(int i) {
    set(i);
  }

  public final int getAndSet(int i) {
    final int old = counter;
    counter = i;
    return old;
  }

  public final boolean compareAndSet(int expect, int update) {
    if (counter == expect) {
      counter = update;
      return true;
    }
    return false;
  }

  public final boolean weakCompareAndSet(int expect, int update) {
    return compareAndSet(expect, update);
  }

  public final int getAndIncrement() {
    final int old = counter;
    counter++;
    return old;
  }

  public final int getAndDecrement() {
    final int old = counter;
    counter--;
    return old;
  }

  public final int getAndAdd(int i) {
    final int old = counter;
    counter += i;
    return old;
  }

  public final int incrementAndGet() {
    return ++counter;
  }

  public final int decrementAndGet() {
    return --counter;
  }

  public final int addAndGet(int i) {
    return counter += i;
  }

  public String toString() {
    return Integer.toString(counter);
  }

  public int intValue() {
    return counter;
  }

  public long longValue() {
    return (long) counter;
  }

  public float floatValue() {
    return (float) counter;
  }

  public double doubleValue() {
    return (double) counter;
  }
}

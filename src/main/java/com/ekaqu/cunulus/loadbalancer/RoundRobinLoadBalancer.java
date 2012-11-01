package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A round robin based load balancer.  Round Robin "selects a resource pointed to by a counter from a list, after which
 * the counter is incremented and if the end is reached, returned to the beginning of the list" -- <a
 * href="http://en.wikipedia.org/wiki/Round-robin">wikipedia</a>
 *
 * @param <E> element type
 */
@Beta
public class RoundRobinLoadBalancer<E> implements LoadBalancer<E> {

  private final AtomicInteger index;

  private RoundRobinLoadBalancer() {
    index = new AtomicInteger(0);
  }

  @VisibleForTesting
  RoundRobinLoadBalancer(final int startingSize) {
    index = new AtomicInteger(startingSize);
  }

  /**
   * Creates a new Round Robin based load balancer
   */
  public static <T> RoundRobinLoadBalancer<T> create() {
    return new RoundRobinLoadBalancer<T>();
  }

  /**
   * Uses Round Robin to select an element from the list.
   * <p/>
   * If the list is null or empty, returns null.
   *
   * @param items
   * @return if list is null or empty returns null, else an element from the list
   */
  @Override
  public E get(@Nullable final List<E> items) {
    if (items == null || items.isEmpty()) return null;

    int thisIndex = Math.abs(index.getAndIncrement()) % items.size();
    return items.get(thisIndex);
  }
}

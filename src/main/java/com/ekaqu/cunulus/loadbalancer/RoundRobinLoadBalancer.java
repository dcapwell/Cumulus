package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A round robin based load balancer
 * @param <E> element type
 */
@Beta
public class RoundRobinLoadBalancer<E> implements LoadBalancer<E> {

  private final AtomicInteger index = new AtomicInteger(0);

  @Override
  public E get(@Nullable final List<E> items) {
    if (items == null || items.isEmpty()) return null;

    int thisIndex = Math.abs(index.getAndIncrement());
    return items.get(thisIndex % items.size());
  }
}

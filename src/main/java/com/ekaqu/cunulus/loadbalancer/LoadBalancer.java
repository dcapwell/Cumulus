package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;

import java.util.List;

/**
 * A Load Balancer will partition the load over all elements.
 *
 * @param <E> element type
 */
@Beta
public interface LoadBalancer<E> {

  /**
   * Given a list of items, returns one based on an algorithm.
   */
  E get(List<E> items);
}

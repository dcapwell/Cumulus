package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;

import java.util.Comparator;
import java.util.List;

/**
 * A load balancer that will return the largest element of a list.  Largest is defined using a {@link Comparator}
 *
 * @param <E> element type
 */
@Beta
public class MaxLoadBalancer<E> extends ComparatorLoadBalancer<E> {

  /**
   * Creates a new MaxLoadBalancer that will return the largest element.
   *
   * @param comparator used to define largest
   */
  public MaxLoadBalancer(final Comparator<E> comparator) {
    super(comparator);
  }

  @Override
  protected E get(final List<E> items, final Comparator<E> comparator) {
    E max = null;
    for (final E item : items) {
      if (max == null) {
        max = item;
      } else if (comparator.compare(max, item) > 0) {
        max = item;
      }
    }
    return max;
  }
}

package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;

import java.util.Comparator;
import java.util.List;

/**
 * A load balancer that will return the smallest element of a list.  Smallest is defined using a {@link Comparator}
 *
 * @param <E> element type
 */
@Beta
public class MinLoadBalancer<E> extends ComparatorLoadBalancer<E> {

  /**
   * Creates a new MinLoadBalancer that will return the smallest element.
   * @param comparator used to define smallest
   */
  public MinLoadBalancer(final Comparator<E> comparator) {
    super(comparator);
  }

  @Override
  protected E get(final List<E> items, final Comparator<E> comparator) {
    E min = null;
    for (final E item : items) {
      if (min == null) {
        min = item;
      } else if (comparator.compare(min, item) < 0) {
        min = item;
      }
    }
    return min;
  }
}

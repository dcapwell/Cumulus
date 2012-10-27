package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A load balancer that will return the largest element of a list.  Largest is defined using a {@link Comparator}
 * <p/>
 * This class uses {@link Collections#sort(java.util.List, java.util.Comparator)} on the provided list.
 *
 * @param <E> element type
 */
@Beta
public class MaxLoadBalancer<E> extends ComparableLoadBalancer<E> {

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
//    E max = null;
//    for (final E item : items) {
//      if (max == null) {
//        max = item;
//      } else if (comparator.compare(max, item) > 0) {
//        max = item;
//      }
//    }
//    return max;
    Collections.sort(items, comparator);
    return items.get(0); // { get(List) should verify that at least one element is in the list
  }
}

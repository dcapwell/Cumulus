package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;
import com.google.common.collect.Ordering;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A load balancer that will return the smallest element of a list.  Smallest is defined using a {@link Comparator}.
 * <p/>
 * This class uses {@link Collections#sort(java.util.List, java.util.Comparator)} on the provided list.
 *
 * @param <E> element type
 */
@Beta
public class MinLoadBalancer<E> extends ComparableLoadBalancer<E> {

  /**
   * Creates a new MinLoadBalancer that will return the smallest element.
   *
   * @param comparator used to define smallest
   */
  private MinLoadBalancer(final Comparator<E> comparator) {
    super(comparator);
  }

  /**
   * Creates a new MinLoadBalancer that will return the smallest element.
   *
   * @param comparator used to define smallest
   */
  public static <E> MinLoadBalancer<E> create(final Comparator<E> comparator) {
    return new MinLoadBalancer<E>(comparator);
  }

  @Override
  protected E get(final List<E> items, final Comparator<E> comparator) {
//    E min = null;
//    for (final E item : items) {
//      if (min == null) {
//        min = item;
//      } else if (comparator.compare(min, item) < 0) {
//        min = item;
//      }
//    }
//    return min;
    Collections.sort(items, comparator);
    return items.get(0); // { get(List) should verify that at least one element is in the list
  }
}

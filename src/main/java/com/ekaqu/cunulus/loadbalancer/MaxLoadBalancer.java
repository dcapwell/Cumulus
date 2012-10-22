package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;

import java.util.Comparator;
import java.util.List;

@Beta
public class MaxLoadBalancer<E> extends ComparatorLoadBalancer<E> {

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

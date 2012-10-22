package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;

import java.util.Comparator;
import java.util.List;

@Beta
public class MinLoadBalancer<E> extends ComparatorLoadBalancer<E> {

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

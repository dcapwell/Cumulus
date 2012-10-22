package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

@Beta
public abstract class ComparatorLoadBalancer<E> implements LoadBalancer<E> {

  private final Comparator<E> comparator;

  public ComparatorLoadBalancer(final Comparator<E> comparator) {
    this.comparator = Preconditions.checkNotNull(comparator);
  }

  @Override
  public E get(@Nullable final List<E> items) {
    if (items == null || items.isEmpty()) return null;

    return get(items, comparator);
  }

  protected abstract E get(final List<E> items, final Comparator<E> comparator);

  public Comparator<E> getComparator() {
    return comparator;
  }
}

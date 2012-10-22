package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

@Beta
public class CollectionLoadBalancer<E> extends ForwardingCollection<E> implements LoadBalancer<E>, Supplier<E> {

  private final Collection<E> items;
  private final LoadBalancer<E> delegateLoadBalancer;
  private final Predicate<E> filterPredicate;

  public CollectionLoadBalancer(final Collection<E> items, final LoadBalancer<E> delegateLoadBalancer, final Predicate<E> filterPredicate) {
    this.filterPredicate = Preconditions.checkNotNull(filterPredicate);
    this.delegateLoadBalancer = Preconditions.checkNotNull(delegateLoadBalancer);
    this.items = Preconditions.checkNotNull(items);
  }

  public CollectionLoadBalancer(final Collection<E> items, final LoadBalancer<E> delegateLoadBalancer) {
    this(items, delegateLoadBalancer, Predicates.<E>alwaysTrue());
  }

  @Override
  protected Collection<E> delegate() {
    return items;
  }

  @Override
  public E get(final List<E> items) {
    return delegateLoadBalancer.get(items);
  }

  @Override
  public E get() {
    if (isEmpty()) {
      return null;
    } else {
      // fitler items into a list
      List<E> filtered = Lists.newArrayList();
      for (final E item : items) {
        if (filterPredicate.apply(item)) {
          filtered.add(item);
        }
      }
      if (filtered.isEmpty()) return null;
      return delegateLoadBalancer.get(filtered);
    }
  }
}

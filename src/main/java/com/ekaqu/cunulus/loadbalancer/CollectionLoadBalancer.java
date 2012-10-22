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

/**
 * A Collection Load Balancer delegates collection operations to a provided collections object and
 * uses that same object while load balancing.  Load balancing is done via {@link com.ekaqu.cunulus.loadbalancer.CollectionLoadBalancer#get()}
 * which will filter out results based off a {@link Predicate} and pass the elements along to {@link CollectionLoadBalancer#get(java.util.List)}.
 *
 * This load balancer also delegates the results to another load balancer
 *
 * @param <E> element type
 */
@Beta
public class CollectionLoadBalancer<E> extends ForwardingCollection<E> implements LoadBalancer<E>, Supplier<E> {

  private final Collection<E> items;
  private final LoadBalancer<E> delegateLoadBalancer;
  private final Predicate<E> filterPredicate;

  /**
   * Creates a new CollectionLoadBalancer that wraps a load balancer around the given collection
   *
   * @param items to load balancer
   * @param delegateLoadBalancer used for all load balancer operations
   * @param filterPredicate used to filter results before load balancing
   */
  public CollectionLoadBalancer(final Collection<E> items, final LoadBalancer<E> delegateLoadBalancer, final Predicate<E> filterPredicate) {
    this.filterPredicate = Preconditions.checkNotNull(filterPredicate);
    this.delegateLoadBalancer = Preconditions.checkNotNull(delegateLoadBalancer);
    this.items = Preconditions.checkNotNull(items);
  }

  /**
   * Creates a new CollectionLoadBalancer that wraps a load balancer around the given collection
   *
   * This constructor does  no filtering of data
   *
   * @param items to load balancer
   * @param delegateLoadBalancer used for all load balancer operations
   */
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

  /**
   * Load Balances elements in a collection.  This will first filter out all elements that don't pass the predicate
   * provided in the constructor then delegates to {@link CollectionLoadBalancer#get(java.util.List)}
   *
   * @return element in the collection or null
   */
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
      return this.get(filtered);
    }
  }
}

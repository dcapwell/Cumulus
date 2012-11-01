package com.ekaqu.cumulus.loadbalancer;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for all load balancers that use a {@link Comparator} for their logic.
 *
 * @param <E> element type
 */
@Beta
public abstract class ComparableLoadBalancer<E> implements LoadBalancer<E> {

  /**
   * Used to compare elements in a list.
   */
  private final Comparator<E> comparator;

  /**
   * Defines the {@link Comparator} to use for the load balancer.
   *
   * @param comparator used to compare elements
   */
  public ComparableLoadBalancer(final Comparator<E> comparator) {
    this.comparator = Preconditions.checkNotNull(comparator);
  }

  /**
   * Delegates load balancing to {@link ComparableLoadBalancer#get(java.util.List, java.util.Comparator)} passing in the
   * comparator provided in the constructor.
   *
   * @param items to load balance
   * @return if items is null or empty then return null, else will call {@link ComparableLoadBalancer#get(java.util.List,
   *         java.util.Comparator)}
   */
  @Override
  public final E get(@Nullable final List<E> items) {
    if (items == null || items.isEmpty()) {
      return null;
    }

    return get(items, comparator);
  }

  /**
   * Comparator used while comparing results for load balancing.
   *
   * @return the comparator used in {@link #get(java.util.List)}
   */
  public final Comparator<E> getComparator() {
    return comparator;
  }

  /**
   * Load balance items using a comparator. It is good practice to not modify the items list but this is not guarantied
   * by this interface.
   *
   * @param items      to load balance
   * @param comparator used to compare elements in the list
   * @return element in the list
   */
  protected abstract E get(final List<E> items, final Comparator<E> comparator);
}

package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A FailOver load balancer will return the same element over and over until a condition is meet.  When this happens,
 * the load balancer will choose a different element and return that.  This cycle repeats as long as there are elements.
 *
 * @param <E> element type
 */
@Beta
public class FailoverLoadBalancer<E> implements LoadBalancer<E> {

  private final AtomicReference<E> current = new AtomicReference<E>();

  private final LoadBalancer<E> delegate;

  /**
   * Creates a new FailoverLoadBalancer that will return the same element over and over until a given condition is met,
   * at which point it will use the delegate loadBalancer to get a new result.
   *
   * @param delegate loadBalancer to fetch new results
   */
  public FailoverLoadBalancer(final LoadBalancer<E> delegate) {
    this.delegate = Preconditions.checkNotNull(delegate);
  }

  /**
   * Returns the same element over and over again until a given condition has been met.  Once this happens
   * it will choose a new element.
   *
   * The following conditions are used to determine "failover":
   * <ul>
   *   <li>no element has been choosen</li>
   *   <li>user notifies load balancer that an exception has happened</li>
   * </ul>
   *
   * If
   * If the current element is not in the provided list, then currently this will return null.
   *
   * @param items to load balance
   * @return if list is null or empty returns null, else an element from the list
   */
  @Override
  public E get(@Nullable final List<E> items) {
    if (items == null || items.isEmpty()) return null;

    E item = current.get();
    // there is no current value, so delegate and set that
    if (item == null) {
      E newItem = delegate.get(items);
      current.set(newItem); // if not null, go with w/e is there
      item = newItem;
    } else {
      if (!items.contains(item)) {
        item = null;
      }
    }
    return item;
  }

  /**
   * Notifies the load balancer of any errors see while using the last returned element.
   *
   * This causes a "failover"
   *
   * @param throwable error thrown while using last returned element
   */
  public void notifyError(Throwable throwable) {
    // fail over by setting current to null
    current.set(null);
  }
}

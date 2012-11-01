package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;

import java.util.List;

/**
 * A Load Balancer chooses resources out of a collection as a means to distribute load more "fairly."
 *
 * @param <E> element type
 */
@Beta
public interface LoadBalancer<E> {

  /**
   * Chooses an single element from the list using a given algorithm.  All returned elements will be from the list
   * provided.
   * <p/>
   * When there are no items in the list, the result should always be null.  Null can also be used if load balancer
   * wishes to not select an element in the list.  That means that even if there are elements in the provided list, the
   * load balancer can choose to reject all of them and return null.
   * <p/>
   * A load balancer is free to modify the order of the list if it chooses (but not remove/add items from the list).
   * This is uncommon but is used in places like {@link MaxLoadBalancer} and {@link MinLoadBalancer}
   */
  E get(List<E> items);
}

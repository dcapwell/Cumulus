package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;
import com.google.common.collect.ForwardingObject;

import java.util.List;

/**
 * Forwarding load balancer that will forward all requests to the underline {@link LoadBalancer}
 *
 * @param <E> type of the load balancer
 */
@Beta
public abstract class ForwardingLoadBalancer<E> extends ForwardingObject implements LoadBalancer<E> {

  /**
   * Defines the underline {@link LoadBalancer} to use
   */
  @Override
  protected abstract LoadBalancer<E> delegate();

  /**
   * Delegates this method to the load balancer provided by {@link com.ekaqu.cunulus.loadbalancer.ForwardingLoadBalancer#delegate()}
   * @param items to load balance
   * @return result of {@code delegate().get(items)}
   */
  @Override
  public E get(final List<E> items) {
    return delegate().get(items);
  }
}

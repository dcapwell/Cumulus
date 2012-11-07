package com.ekaqu.cumulus.loadbalancer;

/**
 * Useful methods for working with load balancers.
 */
public final class LoadBalancers {

  /**
   * Hides constructor from users.
   */
  private LoadBalancers() {
    // do nothing
  }

  /**
   * Create a new load balancer.  This defaults to a round robin based load balancer.
   *
   * @param <E> element type
   * @return new load balancer
   */
  public static <E> LoadBalancer<E> defaultLoadBalancer() {
    return RoundRobinLoadBalancer.create();
  }
}

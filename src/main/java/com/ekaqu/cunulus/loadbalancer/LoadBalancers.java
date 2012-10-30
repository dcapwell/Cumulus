package com.ekaqu.cunulus.loadbalancer;

public class LoadBalancers {

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

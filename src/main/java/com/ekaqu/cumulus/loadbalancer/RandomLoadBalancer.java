package com.ekaqu.cumulus.loadbalancer;

import com.google.common.annotations.Beta;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * A load balancer that randomly picks which elements to return.
 *
 * @param <E> element type
 */
@Beta
public final class RandomLoadBalancer<E> implements LoadBalancer<E> {

  /**
   * Used to find random element of a list.
   */
  private final Random random = new Random(0L);

  /**
   * hides constructor from clients.
   */
  private RandomLoadBalancer() {
  }

  /**
   * Creates a new Random based load balancer.
   *
   * @param <E> load balancer element type
   * @return random LB
   */
  public static <E> RandomLoadBalancer<E> create() {
    return new RandomLoadBalancer<E>();
  }

  /**
   * Randomly selects an element in the list.
   * <p/>
   * If the list is null or empty, returns null.
   *
   * @param items to load balance
   * @return if list is null or empty returns null, else an element from the list
   */
  @Override
  public E get(@Nullable final List<E> items) {
    if (items == null || items.isEmpty()) {
      return null;
    }

    int index = random.nextInt(items.size());
    return items.get(index);
  }
}

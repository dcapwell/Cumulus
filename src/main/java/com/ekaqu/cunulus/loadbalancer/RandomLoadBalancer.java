package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

@Beta
public class RandomLoadBalancer<E> implements LoadBalancer<E> {

  private final Random random = new Random();

  @Override
  public E get(@Nullable final List<E> items) {
    if (items == null || items.isEmpty()) return null;

    int index = random.nextInt(items.size());
    return items.get(index);
  }
}

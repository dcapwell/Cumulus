package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;
import com.google.common.collect.ForwardingObject;

import java.util.List;

@Beta
public abstract class ForwardingLoadBalancer<E> extends ForwardingObject implements LoadBalancer<E> {

  @Override
  protected abstract LoadBalancer<E> delegate();

  @Override
  public E get(final List<E> items) {
    return delegate().get(items);
  }
}

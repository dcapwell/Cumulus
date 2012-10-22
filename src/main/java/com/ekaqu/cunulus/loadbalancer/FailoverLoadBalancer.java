package com.ekaqu.cunulus.loadbalancer;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Beta
public class FailoverLoadBalancer<E> implements LoadBalancer<E> {

  private final AtomicReference<E> current = new AtomicReference<E>();

  private final LoadBalancer<E> delegate;

  public FailoverLoadBalancer(final LoadBalancer<E> delegate) {
    this.delegate = Preconditions.checkNotNull(delegate);
  }

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

  public void notifyError(Throwable throwable) {
    // fail over by setting current to null
    current.set(null);
  }
}

package com.ekaqu.cunulus.net;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class StaticHostAndPortSupplier implements Supplier<HostAndPort> {

  private final List<HostAndPort> addresses;
  private final AtomicInteger counter = new AtomicInteger();

  public StaticHostAndPortSupplier(final List<HostAndPort> addresses) {
    Preconditions.checkArgument(addresses.size() > 0, "Must define at least one address");
    this.addresses = checkNotNull(addresses);
  }

  public static StaticHostAndPortSupplier fromString(final String connections) {
    List<HostAndPort> addresses = Lists.newArrayList();
    for(final String pair : Splitter.on(",").omitEmptyStrings().trimResults().split(connections)) {
      addresses.add(HostAndPort.fromString(pair));
    }
    return new StaticHostAndPortSupplier(addresses);
  }

  @Override
  public HostAndPort get() {
    int thisIndex = Math.abs(counter.getAndIncrement());
    return addresses.get(thisIndex % addresses.size());
  }
}

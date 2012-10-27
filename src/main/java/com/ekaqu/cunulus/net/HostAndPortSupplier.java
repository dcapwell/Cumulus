package com.ekaqu.cunulus.net;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Supplier} for {@link HostAndPort} that uses a list to load balance.
 */
//TODO should this be replaced with CollectionLoadBalancer now that that exists?
@ThreadSafe
public class HostAndPortSupplier implements Supplier<HostAndPort> {

  private final List<HostAndPort> addresses;
  private final AtomicInteger counter = new AtomicInteger();

  /**
   * Creates a new Supplier that load-balances over the provided list
   * @param addresses list of {@link HostAndPort} that must have size greater than zero
   */
  public HostAndPortSupplier(final List<HostAndPort> addresses) {
    Preconditions.checkArgument(addresses.size() > 0, "Must define at least one address");

    this.addresses = ImmutableList.copyOf(addresses);
  }

  /**
   * Creates a new HostAndPortSupplier based off the connection string provided.  All hosts should be
   * splittable by ','.
   */
  public static HostAndPortSupplier fromString(final String connections) {
    List<HostAndPort> addresses = Lists.newArrayList();
    for(final String pair : Splitter.on(",").omitEmptyStrings().trimResults().split(connections)) {
      addresses.add(HostAndPort.fromString(pair));
    }
    return new HostAndPortSupplier(addresses);
  }

  /**
   * Creates a new HostAndPortSupplier based off the connection string provided.  All hosts should be
   * splittable by ','.
   * @param defaultPort defaultPort is connection string doesn't contain one
   */
  public static HostAndPortSupplier fromString(final String connections, final int defaultPort) {
    List<HostAndPort> addresses = Lists.newArrayList();
    for(final String pair : Splitter.on(",").omitEmptyStrings().trimResults().split(connections)) {
      addresses.add(HostAndPort.fromString(pair).withDefaultPort(defaultPort));
    }
    return new HostAndPortSupplier(addresses);
  }

  /**
   * Creates a new HostAndPortSupplier based off the host/port parts.
   */
  public static HostAndPortSupplier fromParts(final String host, final int port) {
    return new HostAndPortSupplier(Arrays.asList(HostAndPort.fromParts(host, port)));
  }

  /**
   * Creates a new HostAndPortSupplier based off the host/port parts.
   */
  public static HostAndPortSupplier of(final HostAndPort... hostAndPort) {
    return new HostAndPortSupplier(Arrays.asList(hostAndPort));
  }

  @Override
  public HostAndPort get() {
    int thisIndex = Math.abs(counter.getAndIncrement());
    return addresses.get(thisIndex % addresses.size());
  }
}

package com.ekaqu.cumulus.net;

import com.ekaqu.cumulus.loadbalancer.LoadBalancer;
import com.ekaqu.cumulus.loadbalancer.RoundRobinLoadBalancer;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link Supplier} for {@link HostAndPort} that uses a list to load balance.
 */
@ThreadSafe
public final class HostAndPortSupplier implements Supplier<HostAndPort> {

  //TODO should this be replaced with CollectionLoadBalancer now that that exists?

  /**
   * Hosts to iterate over.
   */
  private final List<HostAndPort> addresses;

  /**
   * Load balances {@link #addresses}.
   */
  private final LoadBalancer<HostAndPort> loadBalancer = RoundRobinLoadBalancer.create();

  /**
   * Creates a new Supplier that load-balances over the provided list.
   *
   * @param addresses list of {@link HostAndPort} that must have size greater than zero
   */
  private HostAndPortSupplier(final List<HostAndPort> addresses) {
    Preconditions.checkArgument(addresses.size() > 0, "Must define at least one address");

    this.addresses = ImmutableList.copyOf(addresses);
  }

  /**
   * Creates a new HostAndPortSupplier based off the connection string provided.  All hosts should be splittable by
   * ','.
   *
   * @param connections to convert
   * @return supplier
   */
  public static HostAndPortSupplier fromString(final String connections) {
    List<HostAndPort> addresses = Lists.newArrayList();
    for (final String pair : Splitter.on(",").omitEmptyStrings().trimResults().split(connections)) {
      addresses.add(HostAndPort.fromString(pair));
    }
    return new HostAndPortSupplier(addresses);
  }

  /**
   * Creates a new HostAndPortSupplier based off the connection string provided.  All hosts should be splittable by
   * ','.
   *
   * @param connections to convert
   * @param defaultPort defaultPort is connection string doesn't contain one
   * @return supplier
   */
  public static HostAndPortSupplier fromString(final String connections, final int defaultPort) {
    List<HostAndPort> addresses = Lists.newArrayList();
    for (final String pair : Splitter.on(",").omitEmptyStrings().trimResults().split(connections)) {
      addresses.add(HostAndPort.fromString(pair).withDefaultPort(defaultPort));
    }
    return new HostAndPortSupplier(addresses);
  }

  /**
   * Creates a new HostAndPortSupplier based off the host/port parts.
   *
   * @param host host to supply
   * @param port of the host
   * @return supplier
   */
  public static HostAndPortSupplier fromParts(final String host, final int port) {
    return new HostAndPortSupplier(Arrays.asList(HostAndPort.fromParts(host, port)));
  }

  /**
   * Creates a new HostAndPortSupplier based off the host/port parts.
   *
   * @param hostAndPort hosts to supply
   * @return supplier
   */
  public static HostAndPortSupplier of(final HostAndPort... hostAndPort) {
    return new HostAndPortSupplier(Arrays.asList(hostAndPort));
  }

  @Override
  public HostAndPort get() {
//    int thisIndex = Math.abs(counter.getAndIncrement());
//    return addresses.get(thisIndex % addresses.size());
    return loadBalancer.get(addresses);
  }
}

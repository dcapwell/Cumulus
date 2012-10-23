package com.ekaqu.cunulus.pool.mocks;

import com.ekaqu.cunulus.loadbalancer.CollectionLoadBalancer;
import com.ekaqu.cunulus.loadbalancer.RoundRobinLoadBalancer;
import com.ekaqu.cunulus.pool.AbstractObjectFactory;
import com.ekaqu.cunulus.pool.ObjectFactory;
import com.ekaqu.cunulus.util.Factory;
import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Generates strings for testing.  The output is one to ten in that order and once it hits ten it rests back to one.
 */
public class StringObjectFactory extends AbstractObjectFactory<String> implements Factory<String, ObjectFactory<String>> {

  private final List<String> strings = Arrays.asList(
      "one", "two", "three", "four", "five",
      "six", "seven", "eight", "nine", "ten"
  );

  private final CollectionLoadBalancer<String> loadBalancer =
      new CollectionLoadBalancer<String>(strings, new RoundRobinLoadBalancer<String>());

  @Override
  public String get() {
    return loadBalancer.get() + "-" + System.currentTimeMillis();
  }

  @Override
  public ObjectFactory<String> get(final String type) {
    return new StringObjectFactory();
  }
}

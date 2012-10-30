package com.ekaqu.cunulus.loadbalancer;

import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Test(groups = "Unit")
public class MinLoadBalancerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MinLoadBalancerTest.class.getName());

  public void emptyList() {
    LoadBalancer<String> lb = MinLoadBalancer.create(Ordering.<String>natural());
    for(int i = 0; i < 10; i++) {
      final Object result = lb.get(Collections.<String>emptyList());
      Assert.assertNull(result, "Result should be null");
    }
  }

  public void singleElementList() {
    LoadBalancer<String> lb = MinLoadBalancer.create(Ordering.<String>natural());
    final List<String> objects = Arrays.<String>asList("HI");
    for(int i = 0; i < 10; i++) {
      final Object result = lb.get(objects);
      Assert.assertEquals(result, "HI");
    }
  }

  public void multiElementList() {
    LoadBalancer<Integer> lb = MinLoadBalancer.create(Ordering.<Integer>natural());
    final List<Integer> objects = Arrays.<Integer>asList(12, 24, 82, 12, 51, 298, 2, 6982);
    final Integer smallest = lb.get(objects);
    Assert.assertEquals(smallest, Integer.valueOf(2), "Not smallest value");
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void emptyComparable() {
    LoadBalancer<String> lb = MinLoadBalancer.create(null);
  }
}

package com.ekaqu.cunulus.loadbalancer;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = "Unit")
public class RoundRobinLoadBalancerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(RoundRobinLoadBalancerTest.class.getName());

  public void emptyList() {
    LoadBalancer<Object> lb = RoundRobinLoadBalancer.create();
    for(int i = 0; i < 10; i++) {
      final Object result = lb.get(Collections.emptyList());
      Assert.assertNull(result, "Result should be null");
    }
  }

  public void singleElementList() {
    LoadBalancer<Object> lb = RoundRobinLoadBalancer.create();
    final List<Object> objects = Arrays.<Object>asList("HI");
    for(int i = 0; i < 10; i++) {
      final Object result = lb.get(objects);
      Assert.assertEquals(result, "HI");
    }
  }

  public void multiElementList() {
    LoadBalancer<Object> lb = RoundRobinLoadBalancer.create();
    final List<Object> objects = Arrays.<Object>asList("A", "B", "C", "D");

    final Object first = lb.get(objects);
    Assert.assertEquals(first, "A");

    final Object second = lb.get(objects);
    Assert.assertEquals(second, "B");

    final Object third = lb.get(objects);
    Assert.assertEquals(third, "C");

    final Object fourth = lb.get(objects);
    Assert.assertEquals(fourth, "D");

    // resets to A
    final Object fifth = lb.get(objects);
    Assert.assertEquals(fifth, "A");
  }

  /**
   * Tests how the load balancer acts when {@link Integer#MAX_VALUE} is the size of the array
   */
  public void intMax() {
    // since this causes too much memory and time, mock it
    List<Object> list = mock(List.class);
    when(list.size()).thenReturn(Integer.MAX_VALUE);
    when(list.get(Integer.MAX_VALUE - 1)).thenReturn("Found");

    LoadBalancer<Object> lb = new RoundRobinLoadBalancer<Object>(Integer.MAX_VALUE - 2);

    final Object first = lb.get(list);
    Assert.assertNull(first);

    final Object last = lb.get(list);
    Assert.assertEquals(last, "Found");
  }
}

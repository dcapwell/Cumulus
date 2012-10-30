package com.ekaqu.cunulus.loadbalancer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = "Unit")
public class RandomLoadBalancerTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(RandomLoadBalancerTest.class.getName());

  public void emptyList() {
    LoadBalancer<Object> lb = new RandomLoadBalancer<Object>();
    for(int i = 0; i < 10; i++) {
      final Object result = lb.get(Collections.emptyList());
      Assert.assertNull(result, "Result should be null");
    }
  }

  public void singleElementList() {
    LoadBalancer<Object> lb = new RandomLoadBalancer<Object>();
    final List<Object> objects = Arrays.<Object>asList("HI");
    for(int i = 0; i < 10; i++) {
      final Object result = lb.get(objects);
      Assert.assertEquals(result, "HI");
    }
  }

  /**
   * Since Random has a fixed seed, this verifies that the seed is the same between releases.
   */
  public void multiElementList() {
    LoadBalancer<Object> lb = new RandomLoadBalancer<Object>();
    final List<Object> objects = Arrays.<Object>asList("A", "B", "C", "D");

    final Object first = lb.get(objects);
    Assert.assertEquals(first, "C");


    final Object second = lb.get(objects);
    Assert.assertEquals(second, "D");


    final Object third = lb.get(objects);
    Assert.assertEquals(third, "A");

    final Object fourth = lb.get(objects);
    Assert.assertEquals(fourth, "C");

    // resets to A
    final Object fifth = lb.get(objects);
    Assert.assertEquals(fifth, "C");
  }

  public void distrabution() {
    final List<String> objects = Arrays.asList("A", "B", "C", "D");
    LoadBalancer<String> lb = new RandomLoadBalancer<String>();
    Multimap<String, String> distrabution = ArrayListMultimap.create();
    final int iterations = 1000;
    for(int i = 0; i < iterations; i++) {
      final String obj = lb.get(objects);
      distrabution.put(obj, obj);
    }

    final int aCount = distrabution.get("A").size();
    final int bCount = distrabution.get("B").size();
    final int cCount = distrabution.get("C").size();
    final int dCount = distrabution.get("D").size();
    LOGGER.info("Distrabution: {}, {}, {}, {}", aCount, bCount, cCount, dCount);

    List<Integer> distroList = Arrays.asList(aCount, bCount, cCount, dCount);
    int expected = iterations / 4;
    int variation = (int) (iterations * 0.04);
    int max = expected + variation;
    int min = expected - variation;
    LOGGER.info("Expected {}, variation {}", expected, variation);
    for(final int count : distroList) {
      Assert.assertTrue(count >= min && count <= max);
    }
  }
}

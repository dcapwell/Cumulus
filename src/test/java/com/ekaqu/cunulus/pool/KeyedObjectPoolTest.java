package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.pool.mocks.StringObjectFactory;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

@Test(groups = "Unit")
public class KeyedObjectPoolTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(KeyedObjectPoolTest.class.getName());

  private final StringObjectFactory stringFactory = new StringObjectFactory();
  private final PoolBuilder<String>.KeyedPoolBuilder<String, String> poolBuilder = new PoolBuilder<String>()
      .corePoolSize(1)
      .maxPoolSize(2)
      .withKeyType(String.class)
        .factory(stringFactory)
        .keySupplier(stringFactory);

  public void createAndGet() {
    KeyedObjectPool<String, String> pool = (KeyedObjectPool<String, String>) poolBuilder.build();

    final int expectedSize = pool.getCorePoolSize() * pool.getCorePoolSize();

    // verify start size is as expected
    Assert.assertEquals(pool.size(), expectedSize, "Size doesn't match");
    Assert.assertFalse(pool.isEmpty(), "Pool is empty?");

    // borrow everything in the pool
    List<Map.Entry<String,String>> borrowed = Lists.newArrayList();
    for(int i = 0; i < expectedSize; i++) {
      // don't return, want to verify this goes empty
      borrowed.add(pool.borrow().get());
    }

    LOGGER.info("Pool {}", pool);
    LOGGER.info("Borrowed {}", borrowed);
    Assert.assertTrue(pool.isEmpty(), "Pool not empty");
    Assert.assertEquals(pool.size(), 0, "Pool size isn't 0");
    Assert.assertEquals(borrowed.size(), expectedSize, "Borrowed count not starting sized count");

    // add back to the pool
    for(Map.Entry<String, String> e : borrowed) {
      pool.returnToPool(e);
    }

    LOGGER.info("Pool {}", pool);
    Assert.assertFalse(pool.isEmpty(), "Pool is empty");
    Assert.assertEquals(pool.size(), expectedSize, "Pool size isn't core^2");
  }

  public void drainToMax() {
    KeyedObjectPool<String, String> pool = (KeyedObjectPool<String, String>) poolBuilder.build();

    final int coreSize = pool.getCorePoolSize();
    final int maxSize = pool.getMaxPoolSize();

    // borrow everything in the pool
    List<Map.Entry<String,String>> borrowed = Lists.newArrayList();
    for(int i = 0; i < maxSize; i++) {
      // don't return, want to verify this goes empty
      if(i < coreSize) {
        borrowed.add(pool.borrow().get());
      } else {
        // this is here for debugging only.  Lets the debugger jump to where a new task gets created
        borrowed.add(pool.borrow(5, TimeUnit.SECONDS).get());
      }
    }

    LOGGER.info("Pool {}", pool);
    LOGGER.info("Borrowed {}", borrowed);
    Assert.assertTrue(pool.isEmpty(), "Pool not empty");
    Assert.assertEquals(pool.size(), 0, "Pool size isn't 0");
    Assert.assertEquals(borrowed.size(), maxSize, "Borrowed count not starting sized count");

    // pool more, all should be absent
    for(int i = 0; i < maxSize; i++) {
      Assert.assertFalse(pool.borrow().isPresent(), "Pool returned data");
    }

    // return data to pool
    for(Map.Entry<String, String> e : borrowed) {
      pool.returnToPool(e);
    }

    Assert.assertEquals(pool.size(), pool.getMaxPoolSize(), "Pool should be at max size right now");
  }
}

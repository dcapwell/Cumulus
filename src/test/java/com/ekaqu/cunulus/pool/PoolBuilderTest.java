package com.ekaqu.cunulus.pool;

import com.ekaqu.cunulus.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Test(groups = "Unit")
public class PoolBuilderTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(PoolBuilderTest.class.getName());

  public void simplePool() {
    ObjectFactory<String> factory = mock(ObjectFactory.class);
    doReturn("a").when(factory).get();

    Pool<String> pool = new PoolBuilder<String>()
        .objectFactory(factory)
        .build();

    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(pool.size(), 5);

    String value = pool.borrow().get();
    Assert.assertEquals(value, "a");
  }

  public void simpleKeyedPool() {
    ObjectFactory<String> factory = mock(ObjectFactory.class);
    doReturn("a").when(factory).get();

    Factory<String, ObjectFactory<String>> factoryFactory = mock(Factory.class);
    doReturn(factory).when(factoryFactory).get("a");

    KeyedPool<String, String> pool = new PoolBuilder<String>()
        .withKeyType(String.class)
        .keySupplier(factory)
        .factory(factoryFactory)
        .build();

    LOGGER.info("Pool {}", pool);
    Assert.assertEquals(pool.size(), 5);

    String value = pool.borrow().get().getValue();
    Assert.assertEquals(value, "a");
  }
}

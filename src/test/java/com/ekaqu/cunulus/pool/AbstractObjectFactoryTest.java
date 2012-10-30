package com.ekaqu.cunulus.pool;

import org.mockito.Answers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = "Unit")
public class AbstractObjectFactoryTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractObjectFactoryTest.class.getName());

  public void validate() {
    AbstractObjectFactory<String> of = mock(AbstractObjectFactory.class, Answers.CALLS_REAL_METHODS.get());
    Assert.assertEquals(of.validate("hi"), ObjectFactory.State.VALID);
    Assert.assertEquals(of.validate("hi", new Throwable()), ObjectFactory.State.VALID);
  }

  public void rejectExceptions() {
    // given
    AbstractObjectFactory<String> of = mock(AbstractObjectFactory.class, Answers.CALLS_REAL_METHODS.get());
    Throwable valid = new Throwable(), invalid = new Throwable(), close = new Throwable();

    // when
    when(of.validateException(valid)).thenReturn(ObjectFactory.State.VALID);
    when(of.validateException(invalid)).thenReturn(ObjectFactory.State.INVALID);
    when(of.validateException(close)).thenReturn(ObjectFactory.State.CLOSE_POOL);

    // then
    Assert.assertEquals(of.validate("hi", valid), ObjectFactory.State.VALID);
    Assert.assertEquals(of.validate("hi", invalid), ObjectFactory.State.INVALID);
    Assert.assertEquals(of.validate("hi", close), ObjectFactory.State.CLOSE_POOL);
  }

  public void rejectObject() {
    // given
    AbstractObjectFactory<String> of = mock(AbstractObjectFactory.class, Answers.CALLS_REAL_METHODS.get());
    String valid = "valid", invalid = "invalid", close = "close";

    // when
    when(of.validate(valid)).thenReturn(ObjectFactory.State.VALID);
    when(of.validate(invalid)).thenReturn(ObjectFactory.State.INVALID);
    when(of.validate(close)).thenReturn(ObjectFactory.State.CLOSE_POOL);

    // then
    Assert.assertEquals(of.validate(valid, new Throwable()), ObjectFactory.State.VALID);
    Assert.assertEquals(of.validate(invalid, new Throwable()), ObjectFactory.State.INVALID);
    Assert.assertEquals(of.validate(close, new Throwable()), ObjectFactory.State.CLOSE_POOL);
  }
}

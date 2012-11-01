package com.ekaqu.cumulus.retry;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

@Test(groups = "Unit")
public class ExponentialBackOffPolicyTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExponentialBackOffPolicyTest.class.getName());

  public void logTimeRang() {
    ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
    StringBuilder sb = new StringBuilder("Timing:\r\n");
    for(int i = 0; i < 10; i++) {
      sb.append(Strings.padStart(Integer.toString(i), 2, '0')).append("\t\t")
          .append(Strings.padStart(Long.toString(policy.sleepTime(i)), 8, '-')).append("ms\n");
    }
    LOGGER.info(sb.toString());
  }
}

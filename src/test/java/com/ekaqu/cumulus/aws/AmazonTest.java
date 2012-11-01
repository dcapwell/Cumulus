package com.ekaqu.cumulus.aws;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

@Test(groups = "Unit")
public class AmazonTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(AmazonTest.class.getName());

  /**
   * Verifies that the meta data URLs are valid
   */
  @Test(dataProvider = "metaData")
  public void getUrl(final Amazon.MetaData metaData) {
    URL url = metaData.getUrl();
    LOGGER.info("MetaData {} has Url {}", metaData, url);
  }

  @DataProvider
  public static Iterator<Object[]> metaData() {
    List<Object[]> input = Lists.newArrayList();
    for(final Amazon.MetaData e : Amazon.MetaData.values()) {
      input.add(new Object[] {e});
    }
    return input.iterator();
  }
}

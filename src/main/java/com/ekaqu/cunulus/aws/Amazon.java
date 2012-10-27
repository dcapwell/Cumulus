package com.ekaqu.cunulus.aws;

import com.ekaqu.cunulus.retry.Retryer;
import com.ekaqu.cunulus.retry.Retryers;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

public class Amazon {

  /**
   * Meta Data URL for current instances host metadata
   */
  public final static String AWS_METADATA_URL = "http://169.254.169.254/latest/meta-data/";

  /**
   * URL for user defined meta data
   */
  public static final String AWS_USERDATA_URL = "http://169.254.169.254/latest/user-data/";

  /**
   * Enum class for working with ec2 instance meta data.  A complete list of what is available can be found <a href="http://docs.amazonwebservices.com/AWSEC2/2007-03-01/DeveloperGuide/AESDG-chapter-instancedata.html">here</a>.
   * <p/>
   * All docs for each enum is also from the same link.
   */
  public static enum MetaData {

    /**
     * The AMI ID used to launch the instance.
     */
    AMI_ID("ami-id"),

    /**
     * The index of this instance in the reservation (per AMI).
     */
    AMI_LAUNCH_INDEX("ami-launch-index"),

    /**
     * The manifest path of the AMI with which the instance was launched.
     */
    AMI_MANIFEST_PATH("ami-manifest-path"),

    /**
     * The AMI IDs of any instances that were rebundled to create this AMI.
     */
    ANCESTOR_AMI_IDS("ancestor-ami-ids"),

    /**
     * Defines native device names to use when exposing virtual devices.
     */
    BLOCK_DEVICE_MAPPING("block-device-mapping"),

    /**
     * The local hostname of the instance.
     */
    HOSTNAME("hostname"),

    INSTANCE_ACTION("instance-action"),

    /**
     * The ID of this instance.
     */
    INSTANCE_ID("instance-id"),

    /**
     * The type of instance to launch. For more information, see <a href="http://docs.amazonwebservices.com/AWSEC2/2008-08-08/DeveloperGuide/instance-types.html>Instance Types</a>.
     */
    INSTANCE_TYPE("instance-type"),

    /**
     * The local hostname of the instance.
     */
    LOCAL_HOSTNAME("local-hostname"),

    /**
     * Public IP address if launched with direct addressing; private IP address if launched with public addressing.
     */
    LOCAL_IPV4("local-ipv4"),

    /**
     * Mac address.
     */
    MAC("mac"),

    METRICS("metrics"),

    NETWORK_INTERFACES("network/interfaces/"),

    PROFILE("profile"),

    /**
     * The ID of the kernel launched with this instance, if applicable.
     */
    KERNEL_ID("kernel-id"),

    /**
     * The availability zone in which the instance launched.
     */
    AVAILABILITY_ZONE("placement/availability-zone"),

    /**
     * Product codes associated with this instance.
     */
    PRODUCT_CODES("product-codes"),

    /**
     * The public hostname of the instance.
     */
    PUBLIC_HOSTNAME("public-hostname"),

    /**
     * The public IP address.
     */
    PUBLIC_IPV4("public-ipv4"),

    /**
     * Public keys. Only available if supplied at instance launch time.
     */
    PUBLIC_KEYS("public-keys"),

    /**
     * The ID of the RAM disk launched with this instance, if applicable.
     */
    RAMDISK_ID("ramdisk-id"),

    /**
     * ID of the reservation.
     */
    RESERVATION_ID("reservation-id"),

    /**
     * Names of the security groups the instance is launched in. Only available if supplied at instance launch time.
     */
    SECURITY_GROUPS("security-groups");

    private final Retryer defaultRetryer = Retryers.newExponentialBackoffRetryer(3);

    private final String path;

    MetaData(final String path) {
      this.path = path;
    }

    public String getPath() {
      return path;
    }

    /**
     * Get the AWS Meta Data URL
     */
    public URL getUrl() {
      try {
        return new URL(AWS_METADATA_URL + getPath());
      } catch (MalformedURLException e) {
        throw new AssertionError("Unable to create URL from statically know values...");
      }
    }

    /**
     * Does an HTTP request to the AWS MetaData URL to fetch this value
     *
     * @return value from http request
     * @throws IOException when unable to get value from AWS
     */
    public String remoteFetchValue() throws IOException {
      final URL url = getUrl();
      return Resources.toString(url, Charsets.UTF_8);
    }

    /**
     * Does an HTTP request to the AWS MetaData URL to fetch this value.  This will retry based off the retryer defined
     * <p/>
     * This will use ExponentialBackoff and only attempt to retry 3 times
     *
     * @return meta data value
     * @throws IOException when unable to get value from AWS
     */
    public String remoteFetchValueWithRetries() throws IOException {
      return remoteFetchValueWithRetries(defaultRetryer);
    }

    /**
     * Does an HTTP request to the AWS MetaData URL to fetch this value.  This will retry based off the retryer defined
     *
     * @param retryer defines how to retry requests
     * @return meta data value
     * @throws IOException when unable to get value from AWS
     */
    public String remoteFetchValueWithRetries(final Retryer retryer) throws IOException {
      try {
        return retryer.submitWithRetry(new Callable<String>() {
          @Override
          public String call() throws Exception {
            return remoteFetchValue();
          }
        });
      } catch (Exception e) {
        // unable to fetch value, throw error
        if (e instanceof IOException) {
          throw (IOException) e;
        }
        throw new AssertionError("The exception should have been an IOException but was " + e.getClass().getName());
      }
    }

    /**
     * Simple CLI for querying amazon meta data.
     * <p/>
     * Sample request:
     * <p/>
     * java -cp cumulus-0.0.1-SNAPSHOT.jar:guava-13.0.1.jar com.ekaqu.cunulus.aws.Amazon\$MetaData PUBLIC_HOSTNAME
     */
    public static void main(String[] args) throws IOException {
      if (args.length > 0) {
        final String key = args[0].toUpperCase().trim();
        final MetaData metaData = MetaData.valueOf(key);
        final String value = metaData.remoteFetchValueWithRetries();
        System.out.printf("%s => %s\r\n", metaData, value);
      } else {
        StringBuilder sb = new StringBuilder("Please supply a meta data name.  The following are valid:%n");
        for (MetaData e : MetaData.values()) {
          sb.append(e.toString()).append("\r\n");
        }
        System.out.println(sb);
      }
    }
  }
}

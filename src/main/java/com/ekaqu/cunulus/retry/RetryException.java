package com.ekaqu.cunulus.retry;

/**
 * Wrapper exception around an Throwable.  This exception should be used if a runnable throws an exception.
 */
public class RetryException extends RuntimeException {

  /**
   * Creates a new retry exception around the throwable.
   *
   * @param throwable from retry operations
   */
  public RetryException(final Throwable throwable) {
    super(throwable);
  }

  /**
   * Creates a new retry exception with the message and throwable.
   *
   * @param s error message
   * @param throwable from retry operations
   */
  public RetryException(final String s, final Throwable throwable) {
    super(s, throwable);
  }
}

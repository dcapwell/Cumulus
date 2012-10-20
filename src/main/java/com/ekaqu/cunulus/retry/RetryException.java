package com.ekaqu.cunulus.retry;

/**
 * Wrapper exception around an Throwable.  This exception should be used if a runnable throws an exception
 */
public class RetryException extends RuntimeException {

  public RetryException(final Throwable throwable) {
    super(throwable);
  }

  public RetryException(final String s, final Throwable throwable) {
    super(s, throwable);
  }
}

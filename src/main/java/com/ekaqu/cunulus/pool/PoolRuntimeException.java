package com.ekaqu.cunulus.pool;

/**
 * All Pool Exceptions should extend from this or PoolException
 */
public class PoolRuntimeException extends RuntimeException {

  public PoolRuntimeException(final String s) {
    super(s);
  }

  public PoolRuntimeException(final String s, final Throwable throwable) {
    super(s, throwable);
  }

  public PoolRuntimeException(final Throwable throwable) {
    super(throwable);
  }
}

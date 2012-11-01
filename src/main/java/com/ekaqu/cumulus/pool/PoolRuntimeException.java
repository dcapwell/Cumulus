package com.ekaqu.cumulus.pool;

/**
 * All Pool Exceptions should extend from this or PoolException.
 */
public class PoolRuntimeException extends RuntimeException {

  /**
   * New pool execution with message.
   *
   * @param s message
   */
  public PoolRuntimeException(final String s) {
    super(s);
  }

  /**
   * New pool exception with message and throwable.
   *
   * @param s message
   * @param throwable to wrap around
   */
  public PoolRuntimeException(final String s, final Throwable throwable) {
    super(s, throwable);
  }

  /**
   * New pool exception with throwable.
   *
   * @param throwable to wrap around
   */
  public PoolRuntimeException(final Throwable throwable) {
    super(throwable);
  }
}

package com.ekaqu.cunulus.pool;

/**
 * Thrown if the pool is closed
 */
public class ClosedPoolException extends PoolRuntimeException {

  public ClosedPoolException() {
    super("Pool is closed");
  }

  public ClosedPoolException(final String s) {
    super(s);
  }
}

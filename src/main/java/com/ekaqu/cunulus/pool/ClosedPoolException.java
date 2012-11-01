package com.ekaqu.cunulus.pool;

/**
 * Thrown if the pool is closed.
 */
public class ClosedPoolException extends PoolRuntimeException {

  /**
   * New exception that notifies pool is closed.
   */
  public ClosedPoolException() {
    super("Pool is closed");
  }
}

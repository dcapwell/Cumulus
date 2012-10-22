package com.ekaqu.cunulus.pool;

import com.google.common.base.Supplier;

/**
 * This interface is responsible for creating, and cleanup after objects in a pool.
 *
 * @param <T> type of the object this factory generates
 */
public interface ObjectFactory<T> extends Supplier<T> {

  /**
   * Defines the health of a object that belongs to a pool.
   */
  enum State {
    /**
     * The given object is in a good enough state to be regiven back to an Pool
     */
    VALID,
    /**
     * The given object is not in a good state and should be cleaned up
     */
    INVALID,
    /**
     * The pool can no longer be in a good state and should be invalidated
     */
    CLOSE_POOL
  }

  /**
   * Checks the current state of an object to see if it is safe to reuse
   *
   * @param error Exception thrown when last used the object
   * @return current state of the object
   */
  State validate(T obj, Throwable error);

  /**
   * Clean up any resources belonging to the given object
   */
  void cleanup(T obj);
}

package com.ekaqu.cunulus.pool;

import com.google.common.base.Optional;

/**
 * Simplifies creating an ObjectFactory so only get and optional validation
 */
public abstract class AbstractObjectFactory<T> implements ObjectFactory<T> {

  /**
   * This method uses the {@link AbstractObjectFactory#validate(Object)} and {@link AbstractObjectFactory#validateException(Throwable)}
   * methods to validate the given input
   *
   * @see ObjectFactory#validate(Object, com.google.common.base.Optional)
   */
  @Override
  public State validate(final T obj, final Optional<? extends Throwable> error) {
    State state = validate(obj);
    if (error.isPresent()) {
      Throwable throwable = error.get();
      switch (state) {
        case VALID:
          // use the exception's validation
          state = validateException(throwable);
          break;
        case INVALID:
          State thrownState = validateException(throwable);
          if (!State.VALID.equals(thrownState)) {
            // if the exception's state is invalid or close pool, then return that
            state = thrownState;
          }
          break;
        case CLOSE_POOL:
          // do nothing
          break;
        default:
          throw new UnsupportedOperationException();
      }
    }
    return state;
  }

  @Override
  public void cleanup(final T obj) {
    // do nothing
  }


  /**
   * Checks if a given object is safe to reuse
   */
  protected State validate(final T obj) {
    return State.VALID;
  }

  /**
   * Checks a given {@link Throwable} to see if it should invalidate an object
   */
  protected State validateException(final Throwable error) {
    return State.VALID;
  }
}

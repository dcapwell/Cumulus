package com.ekaqu.cumulus.pool;

import com.google.common.base.Preconditions;

/**
 * Simplifies creating an ObjectFactory so only get and optional validation.
 *
 * @param <T> type factory works with
 */
public abstract class AbstractObjectFactory<T> implements ObjectFactory<T> {

  /**
   * This method uses the {@link AbstractObjectFactory#validate(Object)} and {@link
   * AbstractObjectFactory#validateException(Throwable)} methods to validate the given input.
   *
   * @see ObjectFactory#validate(Object, Throwable)
   * @param obj to validate
   * @param error thrown by obj
   * @return what actions to take
   */
  @Override
  public final State validate(final T obj, final Throwable error) {
    State state = Preconditions.checkNotNull(validate(obj));
    if (error != null) {
      switch (state) {
        case VALID:
          // use the exception's validation
          state = validateException(error);
          break;
        case INVALID:
          State thrownState = validateException(error);
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
   * Checks if a given object is safe to reuse.
   *
   * @param obj to validate
   * @return what actions to take
   */
  protected State validate(@SuppressWarnings("UnusedParameters") final T obj) {
    return State.VALID;
  }

  /**
   * Checks a given {@link Throwable} to see if it should invalidate an object.
   *
   * @param error thrown by obj
   * @return what actions to take
   */
  protected State validateException(@SuppressWarnings("UnusedParameters") final Throwable error) {
    return State.VALID;
  }
}

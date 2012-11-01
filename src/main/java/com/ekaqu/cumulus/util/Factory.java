package com.ekaqu.cumulus.util;

import com.google.common.annotations.Beta;

/**
 * A class that can get objects of a single type given a object.
 *
 * @param <T> input needed to make an object
 * @param <V> output object
 */
@Beta
public interface Factory<T, V> {

  /**
   * Get an object of the appropriate type.  The returned value may or may not be a new object.
   *
   * @param type input needed to make an object
   * @return factory generated object
   */
  V get(T type);
}

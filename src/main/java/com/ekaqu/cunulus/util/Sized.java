package com.ekaqu.cunulus.util;

/**
 * Implementing this interface allows an object to indicate that its elements
 * may be efficiently counted. Counting is always less than O(n).
 *
 * @see Java8's Sized Interface
 */
public interface Sized {
  /**
   * A non-negative integer indicating a count of elements.
   *
   * @return non-negative integer indicating a count of elements.
   */
  int size();

  /**
   * Returns {@code true} if the size is zero. May be more efficient than
   * calculating size.
   *
   * @return {@code true} if the size is zero otherwise {@code false}.
   */
  boolean isEmpty();
}

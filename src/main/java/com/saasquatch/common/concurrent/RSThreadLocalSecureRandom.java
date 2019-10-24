package com.saasquatch.common.concurrent;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Similar to {@link ThreadLocalRandom}, but returns {@link SecureRandom}s created by
 * {@link #newFastSecureRandom()}.
 *
 * @author sli
 */
public final class RSThreadLocalSecureRandom {

  private static final List<String> PREFERRED_ALGORITHMS =
      Collections.unmodifiableList(Arrays.asList("NativePRNGNonBlocking", "SHA1PRNG"));

  private static final ThreadLocal<SecureRandom> tl =
      ThreadLocal.withInitial(() -> newFastSecureRandom(PREFERRED_ALGORITHMS));

  private RSThreadLocalSecureRandom() {}

  /**
   * @return The {@link SecureRandom} held by the current thread.
   */
  public static SecureRandom current() {
    return tl.get();
  }

  /**
   * Attempt to create a <em>faster</em> {@link SecureRandom}, or fallback to
   * {@link SecureRandom#SecureRandom() new SecureRandom()} if none of the attempted algorithms is
   * available.
   *
   * @see #current()
   */
  // Visible for testing
  static SecureRandom newFastSecureRandom(List<String> preferredAlgorithms) {
    for (final String algorithm : preferredAlgorithms) {
      try {
        return SecureRandom.getInstance(algorithm);
      } catch (NoSuchAlgorithmException e) {
        // Ignore
      }
    }
    return new SecureRandom();
  }

}

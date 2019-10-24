package com.saasquatch.common.concurrent;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;

/**
 * Similar to {@link ThreadLocalRandom}, but returns {@link SecureRandom}s created by
 * {@link #newFastSecureRandom()}.
 *
 * @author sli
 */
public final class RSThreadLocalSecureRandom {

  private static final String[] PREFERRED_ALGORITHMS = {"NativePRNGNonBlocking", "SHA1PRNG"};
  private static final ThreadLocal<SecureRandom> TL =
      ThreadLocal.withInitial(() -> newSecureRandomForAlgorithms(PREFERRED_ALGORITHMS));

  private RSThreadLocalSecureRandom() {}

  /**
   * @return The {@link SecureRandom} held by the current thread.
   */
  @Nonnull
  public static SecureRandom current() {
    return TL.get();
  }

  /**
   * Attempt to create a {@link SecureRandom} with the algorithms in the given order, or fallback to
   * {@link SecureRandom#SecureRandom() new SecureRandom()} if none of the attempted algorithms is
   * available.
   *
   * @see #current()
   */
  // Visible for testing
  static SecureRandom newSecureRandomForAlgorithms(String... preferredAlgorithms) {
    for (final String algorithm : preferredAlgorithms) {
      try {
        return SecureRandom.getInstance(algorithm);
      } catch (Exception e) {
        // Ignore
      }
    }
    return new SecureRandom();
  }

}

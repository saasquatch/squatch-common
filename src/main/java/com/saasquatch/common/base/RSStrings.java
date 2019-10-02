package com.saasquatch.common.base;

import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utilities for strings
 *
 * @author sli
 */
public final class RSStrings {

  private RSStrings() {}

  /**
   * Convenience method for {@link String#format(Locale, String, Object...)} with
   * {@link Locale#ROOT}
   */
  public static String format(@Nonnull String format, Object... args) {
    return String.format(Locale.ROOT, format, args);
  }

  /**
   * Truncate a String to fit a UTF-8 bytes size.<br>
   * <a href="https://stackoverflow.com/questions/119328">Stack Overflow Source</a>
   */
  public static String truncateToUtf8ByteSize(@Nullable String s, int maxBytes) {
    if (maxBytes < 0)
      throw new IllegalArgumentException();
    if (s == null)
      return null;
    int b = 0;
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      // ranges from http://en.wikipedia.org/wiki/UTF-8
      int skip = 0;
      final int more;
      if (c <= 0x007F) {
        more = 1;
      } else if (c <= 0x07FF) {
        more = 2;
      } else if (c <= 0xD7FF) {
        more = 3;
      } else if (c <= 0xDFFF) {
        // surrogate area, consume next char as well
        more = 4;
        skip = 1;
      } else {
        more = 3;
      }
      b += more;
      if (b > maxBytes) {
        return s.substring(0, i);
      }
      i += skip;
    }
    return s;
  }

}

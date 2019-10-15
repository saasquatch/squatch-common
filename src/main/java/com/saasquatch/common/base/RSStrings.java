package com.saasquatch.common.base;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
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
   * Truncate a String to fit a UTF-8 bytes size
   */
  public static String truncateToUtf8ByteSize(@Nullable String s, int maxBytes) {
    return truncateToByteSize(s, maxBytes, UTF_8);
  }

  /**
   * Truncate a String to fit a byte size for a {@link Charset}
   */
  public static String truncateToByteSize(@Nullable String s, int maxBytes,
      @Nonnull Charset charset) {
    if (maxBytes < 0)
      throw new IllegalArgumentException();
    if (s == null)
      return null;
    final CharBuffer in = CharBuffer.wrap(s);
    final ByteBuffer out = ByteBuffer.allocate(maxBytes);
    final CharsetEncoder encoder = charset.newEncoder();
    encoder.encode(in, out, true);
    out.flip();
    final CharBuffer decoded = charset.decode(out);
    final int decodedLen = decoded.length();
    if (decodedLen == 0) {
      return "";
    } else if (decodedLen == s.length()) {
      // No need to make another copy if we don't need any truncation
      return s;
    } else {
      return decoded.toString();
    }
  }

}

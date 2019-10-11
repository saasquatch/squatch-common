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
  public static String truncateToUtf8ByteSize(@Nullable CharSequence s, int maxBytes) {
    return truncateToByteSize(s, maxBytes, UTF_8);
  }

  /**
   * Truncate a String to fit a byte size for a {@link Charset}
   */
  public static String truncateToByteSize(@Nullable CharSequence s, int maxBytes,
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
    return charset.decode(out).toString();
  }

}

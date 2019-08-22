package saasquatch.common.base;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.BitSet;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Util for URL encoding/decoding. Methods in this class are more standards-compliant and more
 * efficient than Java's built-in URL encoder and decoder. All the URL encoded strings that are to
 * be persisted somewhere should be encoded using {@link #encodeStandard(String)} to keep everything
 * consistent.
 *
 * @author sli
 */
public final class RSUrlCodec {

  // BitSet is mutable. DO NOT MODIFY THEM!!!

  /**
   * RFC 3986 unreserved characters.
   */
  private static final BitSet UNRESERVED = new BitSet(256);
  static {
    UNRESERVED.set('a', 'z' + 1);
    UNRESERVED.set('A', 'Z' + 1);
    UNRESERVED.set('0', '9' + 1);
    "-_.~".chars().forEach(UNRESERVED::set);
  }

  /**
   * www-form-url safe characters.
   */
  private static final BitSet WWW_FORM_URL = new BitSet(256);
  static {
    WWW_FORM_URL.set('a', 'z' + 1);
    WWW_FORM_URL.set('A', 'Z' + 1);
    WWW_FORM_URL.set('0', '9' + 1);
    // special chars
    "-_.*".chars().forEach(WWW_FORM_URL::set);
  }

  /**
   * URL encode with the {@code application/x-www-form-urlencoded} type. This is what Java built-in
   * URLEncoder does by default.
   */
  public static String encodeForm(@Nonnull String s) {
    return encode(s, WWW_FORM_URL, true);
  }

  /**
   * URL encode all characters except for the unreserved ones.<br>
   * This is the standard RFC 3986 behavior. Java's default URLEncoder does not do this because the
   * standard came out in 2005, way after URLEncoder was written.
   */
  public static String encodeStandard(@Nonnull String s) {
    return encode(s, UNRESERVED, false);
  }

  /**
   * URL encode all characters except for the specified chars.
   */
  public static String encode(@Nonnull final String s, @Nonnull final BitSet safeChars,
      final boolean spaceToPlus) {
    Objects.requireNonNull(s);
    final ByteBuffer bytes = UTF_8.encode(s);
    final CharBuffer buf = CharBuffer.allocate(bytes.remaining() * 3);
    while (bytes.hasRemaining()) {
      final int b = bytes.get() & 0xff;
      if (safeChars.get(b)) {
        buf.append((char) b);
      } else if (spaceToPlus && b == ' ') {
        buf.append('+');
      } else {
        buf.append('%');
        final char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
        final char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
        buf.append(hex1);
        buf.append(hex2);
      }
    }
    buf.flip();
    return buf.toString();
  }

  public static String decode(@Nonnull String s) {
    // We want to decode plus to space by default
    return decode(s, true);
  }

  public static String decode(@Nonnull String s, boolean plusToSpace) {
    Objects.requireNonNull(s);
    final ByteBuffer bytes = UTF_8.encode(s);
    final ByteBuffer buf = ByteBuffer.allocate(bytes.remaining());
    while (bytes.hasRemaining()) {
      final byte b = bytes.get();
      if (b == '+' && plusToSpace) {
        buf.put((byte) ' ');
      } else if (b == '%') {
        try {
          final int u = digit16(bytes.get());
          final int l = digit16(bytes.get());
          buf.put((byte) ((u << 4) + l));
        } catch (BufferUnderflowException e) {
          throw new IllegalArgumentException("Invalid URL encoding: ", e);
        }
      } else {
        buf.put(b);
      }
    }
    buf.flip();
    return UTF_8.decode(buf).toString();
  }

  private static int digit16(byte b) {
    final int i = Character.digit((char) b, 16);
    if (i == -1) {
      throw new IllegalArgumentException(
          "Invalid URL encoding: not a valid digit (radix 16): " + b);
    }
    return i;
  }

}

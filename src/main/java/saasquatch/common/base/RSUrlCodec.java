package saasquatch.common.base;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.function.IntPredicate;
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

  private RSUrlCodec() {}

  /**
   * URL encode with the {@code application/x-www-form-urlencoded} type. This is what Java built-in
   * URLEncoder does by default.
   */
  public static String encodeForm(@Nonnull String s) {
    return encode(s, RSUrlCodec::isWwwFormUrlSafe, true);
  }

  /**
   * URL encode all characters except for the unreserved ones.<br>
   * This is the standard RFC 3986 behavior. Java's default URLEncoder does not do this because the
   * standard came out in 2005, way after URLEncoder was written.
   */
  public static String encodeStandard(@Nonnull String s) {
    return encode(s, RSUrlCodec::isRFC3986Unreseved, false);
  }

  /**
   * Convenience method for {@link #encode(String, Charset, IntPredicate, boolean)} with UTF-8
   */
  public static String encode(@Nonnull String s, @Nonnull IntPredicate isSafeChar,
      boolean spaceToPlus) {
    return encode(s, UTF_8, isSafeChar, spaceToPlus);
  }

  /**
   * URL encode all chars except for the safe chars.
   *
   * @param s the input String
   * @param charset the {@link Charset} to use
   * @param isSafeChar a predicate that returns true if the input is considered safe and shouldn't
   *        be encoded
   * @param spaceToPlus whether ' ' should be turned into '+'. Note that isSafeChar takes precedence
   *        over spaceToPlus.
   */
  public static String encode(@Nonnull String s, @Nonnull Charset charset,
      @Nonnull IntPredicate isSafeChar, boolean spaceToPlus) {
    final ByteBuffer bytes = charset.encode(s);
    final CharBuffer buf = CharBuffer.allocate(bytes.remaining() * 3);
    while (bytes.hasRemaining()) {
      final int b = bytes.get() & 0xff;
      if (isSafeChar.test(b)) {
        buf.put((char) b);
      } else if (spaceToPlus && b == ' ') {
        buf.put('+');
      } else {
        buf.put('%').put(hexDigit(b >> 4)).put(hexDigit(b & 0xF));
      }
    }
    buf.flip();
    return buf.toString();
  }

  /**
   * Convenience method for {@link RSUrlCodec#decode(String, boolean)}
   */
  public static String decode(@Nonnull String s) {
    // We want to decode plus to space by default
    return decode(s, true);
  }

  /**
   * Convenience method for {@link RSUrlCodec#decode(String, Charset, boolean)}
   */
  public static String decode(@Nonnull String s, boolean plusToSpace) {
    return decode(s, UTF_8, plusToSpace);
  }

  /**
   * URL decode
   *
   * @param s the input String
   * @param charset the {@link Charset} to use
   * @param plusToSpace whether '+' should be turned into ' '
   * @throws IllegalArgumentException if the input is invalid
   */
  public static String decode(@Nonnull String s, @Nonnull Charset charset, boolean plusToSpace) {
    final CharBuffer chars = CharBuffer.wrap(s);
    final ByteBuffer buf = ByteBuffer.allocate(s.length());
    while (chars.hasRemaining()) {
      final char c = chars.get();
      if (c == '%') {
        try {
          final int u = digit16(chars.get());
          final int l = digit16(chars.get());
          buf.put((byte) ((u << 4) + l));
        } catch (BufferUnderflowException e) {
          throw new IllegalArgumentException("Invalid URL encoding: ", e);
        }
      } else if (plusToSpace && c == '+') {
        buf.put((byte) ' ');
      } else {
        buf.put((byte) c);
      }
    }
    buf.flip();
    return charset.decode(buf).toString();
  }

  private static char hexDigit(final int b) {
    return Character.toUpperCase(Character.forDigit(b & 0xF, 16));
  }

  private static int digit16(char c) {
    final int i = Character.digit(c, 16);
    if (i == -1) {
      throw new IllegalArgumentException(
          "Invalid URL encoding: not a valid digit (radix 16): " + c);
    }
    return i;
  }

  private static boolean isAsciiAlphaNum(int c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
  }

  private static boolean isWwwFormUrlSafe(int c) {
    return isAsciiAlphaNum(c) || c == '-' || c == '_' || c == '.' || c == '*';
  }

  private static boolean isRFC3986Unreseved(int c) {
    return isAsciiAlphaNum(c) || c == '-' || c == '_' || c == '.' || c == '~';
  }

}

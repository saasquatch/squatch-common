package saasquatch.common.base;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.IntPredicate;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Util for URL encoding/decoding. Methods in this class are more standards-compliant and more
 * efficient than Java's built-in URL encoder and decoder. All the URL encoded strings that are to
 * be persisted somewhere should be encoded using {@link #encode(String)} to keep everything
 * consistent.
 *
 * @author sli
 */
public final class RSUrlCodec {

  private RSUrlCodec() {}

  /**
   * @return a singleton {@link Encoder} with the standard RFC 3986 behavior.
   */
  public static Encoder getEncoder() {
    return Encoder.RFC3986;
  }

  /**
   * @return a singleton {@link Encoder} that uses the {@code application/x-www-form-urlencoded}
   *         format. This is what Java's built-in {@link java.net.URLEncoder URLEncoder} does by
   *         default.
   */
  public static Encoder getFormEncoder() {
    return Encoder.FORM;
  }

  /**
   * @return a singleton {@link Decoder#strict() strict} {@link Decoder}.
   */
  public static Decoder getDecoder() {
    return Decoder.STRICT;
  }

  /**
   * @return a singleton {@link Decoder#lenient() lenient} {@link Decoder}
   */
  public static Decoder getLenientDecoder() {
    return Decoder.LENIENT;
  }

  /**
   * URL encode all characters except for the unreserved ones.<br>
   * This is the standard RFC 3986 behavior. Java's default URLEncoder does not do this because the
   * standard came out in 2005, way after URLEncoder was written.
   *
   * @see #getEncoder()
   */
  public static String encode(@Nonnull CharSequence s) {
    return Encoder.RFC3986.encode(s);
  }

  /**
   * URL decode in strict mode
   *
   * @throws IllegalArgumentException if the input contains invalid URL encodings
   * @see #getDecoder()
   * @see Decoder#strict()
   */
  public static String decode(@Nonnull CharSequence s) {
    return Decoder.STRICT.decode(s);
  }

  /**
   * URL Encoder<br>
   * This class is immutable and thread-safe if {@link #safeCharPredicate} is thread-safe. The
   * built-in {@link Encoder}s returned by {@link RSUrlCodec} are thread-safe.
   *
   * @author sli
   * @see RSUrlCodec#getEncoder()
   */
  @Immutable
  public static final class Encoder {

    private static final Encoder RFC3986 =
        new Encoder(UTF_8, RSUrlCodec::isRFC3986Unreseved, false, true);
    private static final Encoder FORM =
        new Encoder(UTF_8, RSUrlCodec::isWwwFormUrlSafe, true, true);

    private final Charset charset;
    private final IntPredicate safeCharPredicate;
    private final boolean spaceToPlus;
    private final boolean upperCase;

    private Encoder(@Nonnull Charset charset, @Nonnull IntPredicate safeCharPredicate,
        boolean spaceToPlus, boolean upperCase) {
      this.charset = charset;
      this.safeCharPredicate = safeCharPredicate;
      this.spaceToPlus = spaceToPlus;
      this.upperCase = upperCase;
    }

    /**
     * @return a new {@link Encoder} with the specified {@link Charset}
     */
    public Encoder withCharset(@Nonnull Charset charset) {
      Objects.requireNonNull(charset);
      if (this.charset.equals(charset)) {
        return this;
      }
      return new Encoder(charset, this.safeCharPredicate, this.spaceToPlus, this.upperCase);
    }

    /**
     * @return a new {@link Encoder} with the specified predicate that determines whether a
     *         character is safe and does not need to be encoded
     */
    public Encoder withSafeCharPredicate(@Nonnull IntPredicate safeCharPredicate) {
      Objects.requireNonNull(safeCharPredicate);
      return new Encoder(this.charset, safeCharPredicate, this.spaceToPlus, this.upperCase);
    }

    /**
     * @param spaceToPlus whether ' ' should be encoded to '+'. If false, ' ' characters are either
     *        left alone or encoded to "%20" depending on the safeCharPredicate. Note that the
     *        safeCharPredicate takes precedence over this.
     *
     * @return a new {@link Encoder} with the specified config
     * @see #withSafeCharPredicate(IntPredicate)
     */
    public Encoder encodeSpaceToPlus(boolean spaceToPlus) {
      if (this.spaceToPlus == spaceToPlus) {
        return this;
      }
      return new Encoder(this.charset, this.safeCharPredicate, spaceToPlus, this.upperCase);
    }

    /**
     * @return a new {@link Encoder} that uses upper case hex digits
     */
    public Encoder upperCase() {
      return withUpperCase(true);
    }

    /**
     * @return a new {@link Encoder} that uses lower case hex digits
     */
    public Encoder lowerCase() {
      return withUpperCase(false);
    }

    private Encoder withUpperCase(boolean upperCase) {
      if (this.upperCase == upperCase) {
        return this;
      }
      return new Encoder(this.charset, this.safeCharPredicate, this.spaceToPlus, upperCase);
    }

    /**
     * URL encode
     */
    public String encode(@Nonnull CharSequence s) {
      final ByteBuffer bytes = charset.encode(toCharBuffer(s));
      final CharBuffer resultBuf = CharBuffer.allocate(bytes.remaining() * 3);
      while (bytes.hasRemaining()) {
        final int b = bytes.get() & 0xFF;
        if (safeCharPredicate.test(b)) {
          resultBuf.put((char) b);
        } else if (spaceToPlus && b == ' ') {
          resultBuf.put('+');
        } else {
          resultBuf.put('%');
          resultBuf.put(hexDigit(b >> 4, upperCase));
          resultBuf.put(hexDigit(b, upperCase));
        }
      }
      resultBuf.flip();
      return resultBuf.toString();
    }

  }

  /**
   * URL decoder<br>
   * This class is immutable and thread-safe.
   *
   * @author sli
   * @see RSUrlCodec#getDecoder()
   */
  @Immutable
  public static final class Decoder {

    private static final Decoder STRICT = new Decoder(UTF_8, true, true);
    private static final Decoder LENIENT = STRICT.lenient();

    private final Charset charset;
    private final boolean plusToSpace;
    private final boolean strict;
    private final byte[] percentBytes;

    private Decoder(@Nonnull Charset charset, boolean plusToSpace, boolean strict) {
      this.charset = charset;
      this.plusToSpace = plusToSpace;
      this.strict = strict;
      this.percentBytes = new String(new char[] {'%'}).getBytes(charset);
    }

    /**
     * @return a new {@link Decoder} with the specified {@link Charset}
     */
    public Decoder withCharset(@Nonnull Charset charset) {
      Objects.requireNonNull(charset);
      if (this.charset.equals(charset)) {
        return this;
      }
      return new Decoder(charset, this.plusToSpace, this.strict);
    }

    /**
     * @param plusToSpace whether '+' should be decoded to ' '. If not, '+' characters will be left
     *        alone.
     * @return a new {@link Decoder} with the specified config
     */
    public Decoder decodePlusToSpace(boolean plusToSpace) {
      if (this.plusToSpace == plusToSpace) {
        return this;
      }
      return new Decoder(this.charset, plusToSpace, this.strict);
    }

    /**
     * @return a new {@link Decoder} in strict mode, meaning invalid URL encodings will cause an
     *         {@link IllegalArgumentException}
     */
    public Decoder strict() {
      return withStrict(true);
    }

    /**
     * @return a new {@link Decoder} in lenient mode, meaning invalid URL encodings will be ignored
     */
    public Decoder lenient() {
      return withStrict(false);
    }

    private Decoder withStrict(boolean strict) {
      if (this.strict == strict) {
        return this;
      }
      return new Decoder(this.charset, this.plusToSpace, strict);
    }

    /**
     * URL decode
     */
    public String decode(@Nonnull CharSequence s) {
      final CharBuffer chars = toCharBuffer(s);
      final CharBuffer resultBuf = CharBuffer.allocate(s.length());
      while (chars.hasRemaining()) {
        final char c = chars.get();
        if (c == '%') {
          /*
           * We hit a '%', and in order to preserve unsafe characters, we need to process all the
           * consecutive % sequences and turn those into one single byte array.
           */
          if (chars.remaining() < 2) {
            // underflow
            if (strict) {
              throw new IllegalArgumentException(
                  "Invalid URL encoding: Incomplete trailing escape (%) pattern");
            }
            resultBuf.put('%');
            continue;
          }
          chars.position(chars.position() - 1);
          // Assuming we are only left with % sequences, we need to allocate remaining / 3.
          final ByteBuffer decBuf = ByteBuffer.allocate(chars.remaining() / 3);
          do {
            if (chars.get() != '%') {
              // The % sequences ended. Rewind one char and bail out.
              chars.position(chars.position() - 1);
              break;
            }
            final char uc = chars.get();
            final char lc = chars.get();
            final int u = digit16(uc);
            final int l = digit16(lc);
            if (u != -1 && l != -1) {
              // Both digits are valid
              decBuf.put((byte) ((u << 4) + l));
            } else if (strict) {
              // We have an invalid digit and we are in strict mode
              throw new IllegalArgumentException("Invalid URL encoding: "
                  + "Illegal hex characters in escape (%) pattern: %" + uc + lc);
            } else {
              /*
               * The sequence has an invalid digit, so we need to output the '%' and rewind, since
               * the 2 characters can potentially start a new encoding sequence.
               */
              decBuf.put(percentBytes);
              chars.position(chars.position() - 2);
              break;
            }
          } while (chars.remaining() > 2);
          // The byte array has been built. Now decode it and output it.
          decBuf.flip();
          if (decBuf.hasRemaining()) {
            resultBuf.put(charset.decode(decBuf));
          }
        } else if (plusToSpace && c == '+') {
          resultBuf.put(' ');
        } else {
          resultBuf.put(c);
        }
      }
      resultBuf.flip();
      return resultBuf.toString();
    }

  }

  private static CharBuffer toCharBuffer(@Nonnull CharSequence s) {
    if (s instanceof CharBuffer) {
      return (CharBuffer) s;
    } else {
      return CharBuffer.wrap(s);
    }
  }

  private static char hexDigit(int b, boolean upperCase) {
    final int digit = b & 0xF;
    if (digit < 10) {
      return (char) ('0' + digit);
    }
    return (char) ((upperCase ? 'A' : 'a') - 10 + digit);
  }

  private static int digit16(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    } else if (c >= 'A' && c <= 'F') {
      return c + 10 - 'A';
    } else if (c >= 'a' && c <= 'f') {
      return c + 10 - 'a';
    } else {
      return -1;
    }
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

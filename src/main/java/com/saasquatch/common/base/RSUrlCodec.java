package com.saasquatch.common.base;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import com.saasquatch.common.collect.RSCollectors;

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
   * URL decode in strict mode with UTF-8
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

    /**
     * A {@link Set} {@link Charset}s that are supersets of ASCII. It's not necessarily an
     * exhaustive list and it's only used for optimization.
     */
    private static final Set<Charset> SINGLE_BYTE_ASCII_SUPERSETS =
        Stream.of(UTF_8, US_ASCII, ISO_8859_1).collect(RSCollectors.toUnmodifiableSet());

    private static final Encoder RFC3986 =
        new Encoder(UTF_8, InternalSafeCharPredicate.RFC3986_UNRESERVED, false, true);
    private static final Encoder FORM =
        new Encoder(UTF_8, InternalSafeCharPredicate.WWW_FORM_URLENCODED, true, true);

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
     * @param safeCharPredicate predicate that determines whether a character is safe and does not
     *        need to be encoded.<br>
     *        Note that this predicate is expected to be:
     *        <ul>
     *        <li>Stateless without side effects.</li>
     *        <li>The resulting encoded strings only contain ASCII characters, which is the RFC
     *        standard for URLs.</li>
     *        </ul>
     *        Failing to meet these expectations may cause unexpected behaviors.
     * @return a new {@link Encoder} with the specified predicate
     */
    public Encoder withSafeCharPredicate(@Nonnull IntPredicate safeCharPredicate) {
      Objects.requireNonNull(safeCharPredicate);
      // When a custom safeCharPredicate is provided, stop assuming optimal
      return new Encoder(this.charset, safeCharPredicate, this.spaceToPlus, this.upperCase);
    }

    /**
     * @param spaceToPlus whether ' ' should be encoded to '+'. If false, ' ' characters are either
     *        left alone or encoded depending on the safeCharPredicate. Note that the
     *        safeCharPredicate takes precedence over this, meaning that if ' ' is considered safe
     *        by safeCharPredicate, this method does nothing.
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
      // Only trust InternalSafeCharPredicate
      if (SINGLE_BYTE_ASCII_SUPERSETS.contains(charset)
          && safeCharPredicate instanceof InternalSafeCharPredicate) {
        return encodeOptimal(s);
      } else {
        return encodeNonOptimal(s);
      }
    }

    /**
     * A highly optimized version of doing URL encoding that only works if:
     * <ul>
     * <li>The {@link Charset} is single-byte and is a superset of ASCII</li>
     * <li>The safeCharPredicate always encodes non-ASCII characters</li>
     * </ul>
     * If any of the conditions above is not met, this method does not work. Instead,
     * {@link #encodeNonOptimal(CharSequence)}, which is slightly slower, should be used.
     */
    private String encodeOptimal(@Nonnull CharSequence s) {
      final ByteBuffer bytes = charset.encode(CharBuffer.wrap(s));
      // One byte can at most be turned into 3 chars
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

    /**
     * A more compatible and resilient version of doing URL encoding that handles weird
     * {@link Charset}s and safeCharPredicates that are not standards compliant.
     *
     * @see #encodeOptimal(CharSequence)
     */
    private String encodeNonOptimal(@Nonnull CharSequence s) {
      final CharBuffer chars = CharBuffer.wrap(s);
      /*
       * Not using CharBuffer since it's hard to predict how many characters we will end up having
       * depending on the charsets.
       */
      final StringBuilder resultBuf = new StringBuilder(chars.remaining() * 3);
      // The buffer used for encoding sequences. It will be reused for all the encoding sequences.
      final CharBuffer encBuf = CharBuffer.allocate(chars.remaining());
      while (chars.hasRemaining()) {
        final char c = chars.get();
        if (safeCharPredicate.test(c)) {
          // Got a safe char. Output it.
          resultBuf.append(c);
        } else if (spaceToPlus && c == ' ') {
          // Got a space and this encoder is set to encode space to plus
          resultBuf.append('+');
        } else {
          /*
           * We hit a char that needs to be encoded. Keep going until we hit another safe char or
           * space and encode all those chars together.
           */
          chars.position(chars.position() - 1);
          // Clear the buffer
          encBuf.clear();
          encSequenceLoop: do {
            final char encChar = chars.get();
            if (safeCharPredicate.test(encChar) || (spaceToPlus && encChar == ' ')) {
              chars.position(chars.position() - 1);
              break encSequenceLoop;
            }
            encBuf.put(encChar);
          } while (chars.hasRemaining());
          // Encode the sequence together
          encBuf.flip();
          for (final byte b : encBuf.toString().getBytes(charset)) {
            resultBuf.append('%');
            resultBuf.append(hexDigit(b >> 4, upperCase));
            resultBuf.append(hexDigit(b, upperCase));
          }
        }
      }
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

    private Decoder(@Nonnull Charset charset, boolean plusToSpace, boolean strict) {
      this.charset = charset;
      this.plusToSpace = plusToSpace;
      this.strict = strict;
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
      final CharBuffer chars = CharBuffer.wrap(s);
      final CharBuffer resultBuf = CharBuffer.allocate(s.length());
      /*
       * Buffer used for decoding one set of % patterns. Assuming the entire input only consists of
       * % patterns, we only need its length / 3 bytes. This will be reused for all the patterns.
       */
      final ByteBuffer decBuf = ByteBuffer.allocate(s.length() / 3);
      mainCharsLoop: while (chars.hasRemaining()) {
        final char c = chars.get();
        if (c == '%') {
          /*
           * We hit a '%', and in order to preserve unsafe characters, we need to process all the
           * consecutive % patterns and turn those into one single byte array.
           */
          if (chars.remaining() < 2) {
            // Underflow. Error if strict.
            if (strict) {
              throw new IllegalArgumentException(
                  "Invalid URL encoding: Incomplete trailing escape (%) pattern");
            }
            resultBuf.put('%');
            continue mainCharsLoop;
          }
          // Rewind to put the '%' back
          chars.position(chars.position() - 1);
          // Clear the buffer
          decBuf.clear();
          // Flag for whether we hit an invalid digit. Only applicable for lenient mode.
          boolean hasInvalidDigit = false;
          escapePatternLoop: do {
            if (chars.get() != '%') {
              // The % pattern ended. Rewind one char and bail out.
              chars.position(chars.position() - 1);
              break escapePatternLoop;
            }
            final char uc = chars.get();
            final char lc = chars.get();
            final int u = Character.digit(uc, 16);
            final int l = Character.digit(lc, 16);
            if (u != -1 && l != -1) {
              // Both digits are valid
              decBuf.put((byte) ((u << 4) + l));
            } else if (strict) {
              // We have an invalid digit and we are in strict mode
              throw new IllegalArgumentException("Invalid URL encoding: "
                  + "Illegal hex characters in escape (%) pattern: %" + uc + lc);
            } else {
              /*
               * The pattern has an invalid digit, so we need to signal to output the '%' and
               * rewind, since the 2 characters can potentially start a new encoding pattern.
               */
              hasInvalidDigit = true;
              chars.position(chars.position() - 2);
              break escapePatternLoop;
            }
          } while (chars.remaining() >= 3);
          // The byte array has been built. Now decode it and output it.
          decBuf.flip();
          if (decBuf.hasRemaining()) {
            resultBuf.put(charset.decode(decBuf));
          }
          if (hasInvalidDigit) {
            /*
             * If we hit an invalid digit, we need to output the leading '%' and leave the digits
             * alone
             */
            resultBuf.put('%');
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

  private static char hexDigit(int b, boolean upperCase) {
    final int digit = b & 0xF;
    if (digit < 10) {
      return (char) ('0' + digit);
    }
    return (char) ((upperCase ? 'A' : 'a') - 10 + digit);
  }

  private static boolean isAsciiAlphaNum(int c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
  }

  private static enum InternalSafeCharPredicate implements IntPredicate {

    RFC3986_UNRESERVED {
      @Override
      public boolean test(int c) {
        return isAsciiAlphaNum(c) || c == '-' || c == '_' || c == '.' || c == '~';
      }
    },

    WWW_FORM_URLENCODED {
      @Override
      public boolean test(int c) {
        return isAsciiAlphaNum(c) || c == '-' || c == '_' || c == '.' || c == '*';
      }
    },;

  }

}

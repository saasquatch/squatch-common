package saasquatch.common.base;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.IntPredicate;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import saasquatch.common.annotations.RSBeta;

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
   * @deprecated use {@link #encode(CharSequence)}
   */
  @Deprecated
  public static String encodeStandard(@Nonnull CharSequence s) {
    return Encoder.RFC3986.encode(s);
  }

  /**
   * URL encode with the {@code application/x-www-form-urlencoded} type. This is what Java built-in
   * URLEncoder does by default.
   *
   * @deprecated use {@link #getFormEncoder()}
   */
  @Deprecated
  public static String encodeForm(@Nonnull CharSequence s) {
    return Encoder.FORM.encode(s);
  }

  /**
   * @deprecated use {@link #getEncoder()}
   */
  @Deprecated
  public static String encode(@Nonnull CharSequence s, @Nonnull IntPredicate isSafeChar,
      boolean spaceToPlus) {
    return Encoder.RFC3986.withSafeCharPredicate(isSafeChar).encodeSpaceToPlus(spaceToPlus)
        .encode(s);
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
   * @deprecated use {@link #getDecoder()}
   */
  @Deprecated
  public static String decode(@Nonnull CharSequence s, boolean plusToSpace) {
    return Decoder.STRICT.decodePlusToSpace(plusToSpace).decode(s);
  }

  /**
   * @deprecated use {@link #getLenientDecoder()}
   */
  @Deprecated
  public static String decodeLenient(@Nonnull CharSequence s) {
    return Decoder.LENIENT.decode(s);
  }

  /**
   * @deprecated use {@link #getDecoder()}
   */
  @Deprecated
  public static String decodeLenient(@Nonnull CharSequence s, boolean plusToSpace) {
    return Decoder.LENIENT.decodePlusToSpace(plusToSpace).decode(s);
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
      final ByteBuffer bytes = charset.encode(CharBuffer.wrap(s));
      final int bytesLen = bytes.remaining(); // record the length here
      final PrimitiveIterator.OfInt bytesIter = new PrimitiveIterator.OfInt() {

        @Override
        public boolean hasNext() {
          return bytes.hasRemaining();
        }

        @Override
        public int nextInt() {
          try {
            return bytes.get();
          } catch (BufferUnderflowException e) {
            throw new NoSuchElementException(e.getMessage());
          }
        }

      };
      final PrimitiveIterator.OfInt encodedCharIterator = encode(bytesIter);
      final CharBuffer buf = CharBuffer.allocate(bytesLen * 3);
      while (encodedCharIterator.hasNext()) {
        buf.put((char) encodedCharIterator.nextInt());
      }
      buf.flip();
      return buf.toString();
    }

    @RSBeta
    public PrimitiveIterator.OfInt encode(@Nonnull PrimitiveIterator.OfInt byteIterator) {
      return new PrimitiveIterator.OfInt() {

        /**
         * Buffer for the final output
         */
        private final CharBuffer resultBuffer;
        {
          resultBuffer = CharBuffer.allocate(3);
          resultBuffer.flip(); // make sure it's in read mode by default
        }

        @Override
        public boolean hasNext() {
          tryProcess();
          return resultBuffer.hasRemaining();
        }

        @Override
        public int nextInt() {
          tryProcess();
          try {
            return resultBuffer.get();
          } catch (BufferUnderflowException e) {
            throw new NoSuchElementException(e.getMessage());
          }
        }

        private void tryProcess() {
          if (resultBuffer.hasRemaining() || !byteIterator.hasNext()) {
            // No need to proceed if the result buffer is not empty or the source is already empty
            return;
          }
          resultBuffer.clear();
          try {
            final int b = byteIterator.nextInt() & 0xFF;
            if (safeCharPredicate.test(b)) {
              resultBuffer.put((char) b);
            } else if (spaceToPlus && b == ' ') {
              resultBuffer.put('+');
            } else {
              resultBuffer.put('%');
              resultBuffer.put(hexDigit(b >> 4, upperCase));
              resultBuffer.put(hexDigit(b, upperCase));
            }
          } finally {
            resultBuffer.flip();
          }
        }

      };
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
      final PrimitiveIterator.OfInt decodedBytes = decode(s.chars().iterator());
      final ByteBuffer buf = ByteBuffer.allocate(s.length());
      while (decodedBytes.hasNext()) {
        buf.put((byte) decodedBytes.nextInt());
      }
      buf.flip();
      return charset.decode(buf).toString();
    }

    @RSBeta
    public PrimitiveIterator.OfInt decode(@Nonnull PrimitiveIterator.OfInt charIterator) {
      return new PrimitiveIterator.OfInt() {

        /**
         * Buffer for pushing back invalid characters
         */
        private final CharBuffer pushbackBuffer;
        {
          if (strict) { // no need for pushback in strict mode
            pushbackBuffer = null;
          } else {
            pushbackBuffer = CharBuffer.allocate(2);
            pushbackBuffer.flip(); // make sure it's in read mode by default
          }
        }


        /**
         * Buffer for the final output
         */
        private final ByteBuffer resultBuffer;
        {
          resultBuffer = ByteBuffer.allocate(2);
          resultBuffer.flip(); // make sure it's in read mode by default
        }

        @Override
        public boolean hasNext() {
          tryProcess();
          return resultBuffer.hasRemaining();
        }

        @Override
        public int nextInt() {
          tryProcess();
          try {
            return resultBuffer.get();
          } catch (BufferUnderflowException e) {
            throw new NoSuchElementException(e.getMessage());
          }
        }

        private boolean _hasNextChar() {
          if (pushbackBuffer != null && pushbackBuffer.hasRemaining()) {
            return true;
          }
          return charIterator.hasNext();
        }

        private char _nextChar() {
          if (pushbackBuffer != null && pushbackBuffer.hasRemaining()) {
            return pushbackBuffer.get();
          }
          return (char) charIterator.nextInt();
        }

        private void handleSeqStrict() {
          try {
            final int u = digit16Strict(_nextChar());
            final int l = digit16Strict(_nextChar());
            resultBuffer.put((byte) ((u << 4) + l));
          } catch (BufferUnderflowException | NoSuchElementException e) {
            throw new IllegalArgumentException(
                "Invalid URL encoding: Incomplete trailing escape (%) pattern", e);
          }
        }

        private void handleSeqLenient() {
          final char uc, lc;
          try {
            uc = _nextChar();
          } catch (BufferUnderflowException | NoSuchElementException e) {
            // We reached the end unexpectedly, so we need to output '%'.
            resultBuffer.put((byte) '%');
            return;
          }
          try {
            lc = _nextChar();
          } catch (BufferUnderflowException | NoSuchElementException e) {
            // We reached the end unexpectedly, so we need to output '%' and the first char read.
            resultBuffer.put((byte) '%').put((byte) uc);
            return;
          }
          final int u = digit16(uc);
          final int l = digit16(lc);
          if (u != -1 && l != -1) {
            // Both digits are valid.
            resultBuffer.put((byte) ((u << 4) + l));
          } else {
            /*
             * The sequence has an invalid digit, so we need to output the '%' and rewind, since the
             * 2 characters can potentially start a new encoding sequence.
             */
            resultBuffer.put((byte) '%');
            pushbackBuffer.clear();
            try {
              pushbackBuffer.put(uc).put(lc);
            } finally {
              pushbackBuffer.flip();
            }
          }
        }

        private void tryProcess() {
          if (resultBuffer.hasRemaining() || !_hasNextChar()) {
            // No need to proceed if the result buffer is not empty or the source is already empty
            return;
          }
          resultBuffer.clear();
          try {
            final char c = _nextChar();
            if (c == '%') {
              if (strict) {
                handleSeqStrict();
              } else {
                handleSeqLenient();
              }
            } else if (plusToSpace && c == '+') {
              resultBuffer.put((byte) ' ');
            } else {
              resultBuffer.put((byte) c);
            }
          } finally {
            resultBuffer.flip();
          }
        }

      };
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

  private static int digit16Strict(char c) {
    final int i = digit16(c);
    if (i == -1) {
      throw new IllegalArgumentException(
          "Invalid URL encoding: Illegal hex characters in escape (%) pattern: " + c);
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

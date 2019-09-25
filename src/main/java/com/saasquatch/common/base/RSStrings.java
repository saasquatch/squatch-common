package com.saasquatch.common.base;

/**
 * Utilities for strings
 *
 * @author sli
 */
public final class RSStrings {

  private RSStrings() {}

  /**
   * Truncate a String to fit a UTF-8 bytes size.<br>
   * Source:
   * https://stackoverflow.com/questions/119328/how-do-i-truncate-a-java-string-to-fit-in-a-given-number-of-bytes-once-utf-8-en
   */
  public static String truncateToUtf8ByteSize(String s, int maxBytes) {
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

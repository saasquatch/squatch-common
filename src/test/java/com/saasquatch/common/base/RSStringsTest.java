package com.saasquatch.common.base;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public class RSStringsTest {

  @Test
  public void testLocaleRootFormat() {
    assertEquals("1.500000", RSStrings.format("%f", 1.5));
  }

  /**
   * <a href="https://stackoverflow.com/questions/119328">Stack Overflow Source</a>
   */
  private static void _testUtf8TruncationExample(String s, int maxBytes, int expectedBytes) {
    String result = RSStrings.truncateToUtf8ByteSize(s, maxBytes);
    byte[] utf8 = result.getBytes(UTF_8);
    assertTrue(utf8.length <= maxBytes, "BAD: our truncation of " + s + " was too big");
    assertEquals(expectedBytes, utf8.length,
        "BAD: expected " + expectedBytes + " got " + utf8.length);
    // System.out.println(s + " truncated to " + result);
  }

  /**
   * <a href="https://stackoverflow.com/questions/119328">Stack Overflow Source</a>
   */
  @Test
  public void testUtf8TruncationExamples() {
    _testUtf8TruncationExample("abcd", 0, 0);
    _testUtf8TruncationExample("abcd", 1, 1);
    _testUtf8TruncationExample("abcd", 2, 2);
    _testUtf8TruncationExample("abcd", 3, 3);
    _testUtf8TruncationExample("abcd", 4, 4);
    _testUtf8TruncationExample("abcd", 5, 4);

    _testUtf8TruncationExample("a\u0080b", 0, 0);
    _testUtf8TruncationExample("a\u0080b", 1, 1);
    _testUtf8TruncationExample("a\u0080b", 2, 1);
    _testUtf8TruncationExample("a\u0080b", 3, 3);
    _testUtf8TruncationExample("a\u0080b", 4, 4);
    _testUtf8TruncationExample("a\u0080b", 5, 4);

    _testUtf8TruncationExample("a\u0800b", 0, 0);
    _testUtf8TruncationExample("a\u0800b", 1, 1);
    _testUtf8TruncationExample("a\u0800b", 2, 1);
    _testUtf8TruncationExample("a\u0800b", 3, 1);
    _testUtf8TruncationExample("a\u0800b", 4, 4);
    _testUtf8TruncationExample("a\u0800b", 5, 5);
    _testUtf8TruncationExample("a\u0800b", 6, 5);

    // surrogate pairs
    _testUtf8TruncationExample("\uD834\uDD1E", 0, 0);
    _testUtf8TruncationExample("\uD834\uDD1E", 1, 0);
    _testUtf8TruncationExample("\uD834\uDD1E", 2, 0);
    _testUtf8TruncationExample("\uD834\uDD1E", 3, 0);
    _testUtf8TruncationExample("\uD834\uDD1E", 4, 4);
    _testUtf8TruncationExample("\uD834\uDD1E", 5, 4);
  }

  @Test
  public void testUtf8Truncation() {
    for (int i = 0; i < 1024; i++) {
      final String s = RandomStringUtils.random(1024);
      assertTrue(s.getBytes(UTF_8).length > 512);
      String truncated = RSStrings.truncateToUtf8ByteSize(s, 512);
      final int truncatedUtf8Size = truncated.getBytes(UTF_8).length;
      assertTrue(truncatedUtf8Size <= 512, "We should never exceed the limit");
      assertTrue(truncatedUtf8Size > 512 - 4, "We should be at most 4 bytes off");
    }
  }

  @Test
  public void testUtf8TruncationBadInput() {
    // This should not error
    assertNull(RSStrings.truncateToUtf8ByteSize(null, 123));
    assertThrows(IllegalArgumentException.class, () -> RSStrings.truncateToUtf8ByteSize(null, -1),
        "negative input should error");
  }

}

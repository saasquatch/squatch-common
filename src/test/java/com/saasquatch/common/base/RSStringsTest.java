package com.saasquatch.common.base;

import static java.nio.charset.StandardCharsets.UTF_16BE;
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
  public void testTruncatingFullChar() {
    assertEquals("ab", RSStrings.truncateToByteSize("abc", 5, UTF_16BE));
  }

  @Test
  public void testUtf8TruncationBadInput() {
    // This should not error
    assertNull(RSStrings.truncateToUtf8ByteSize(null, 123));
    assertThrows(IllegalArgumentException.class, () -> RSStrings.truncateToUtf8ByteSize(null, -1),
        "negative input should error");
  }

}

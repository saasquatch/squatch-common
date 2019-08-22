package saasquatch.common.base;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class RSStringsTests {

  @Test
  public void testUtf8Truncation() {
    for (int i = 0; i < 1024; i++) {
      final String s = RandomStringUtils.random(1024);
      assertTrue(s.getBytes(UTF_8).length > 512);
      String truncated = RSStrings.truncateToUtf8ByteSize(s, 512);
      final int truncatedUtf8Size = truncated.getBytes(UTF_8).length;
      assertTrue("We should never exceed the limit", truncatedUtf8Size <= 512);
      assertTrue("We should be at most 4 bytes off", truncatedUtf8Size > 512 - 4);
    }
  }

}

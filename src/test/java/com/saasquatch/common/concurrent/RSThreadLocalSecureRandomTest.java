package com.saasquatch.common.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

public class RSThreadLocalSecureRandomTest {

  @Test
  public void testWorks() {
    assertNotNull(RSThreadLocalSecureRandom.current());
    if (SystemUtils.IS_OS_LINUX) {
      assertEquals("NativePRNGNonBlocking", RSThreadLocalSecureRandom.current().getAlgorithm());
    } else if (SystemUtils.IS_OS_WINDOWS) {
      assertEquals("SHA1PRNG", RSThreadLocalSecureRandom.current().getAlgorithm());
    }
  }

  @Test
  public void testNewFastSecureRandom() {
    assertNotNull(RSThreadLocalSecureRandom.newSecureRandomForAlgorithms("   ",
        "ThisIsDefinitelyNotARealAlgorithm", "NativePRNGNonBlocking", "SHA1PRNG"));
    assertNotNull(RSThreadLocalSecureRandom.newSecureRandomForAlgorithms(new String[0]));
    assertNotNull(RSThreadLocalSecureRandom.newSecureRandomForAlgorithms("???"));
    assertNotNull(RSThreadLocalSecureRandom.newSecureRandomForAlgorithms((String) null));
  }

}

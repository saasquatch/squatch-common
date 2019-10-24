package com.saasquatch.common.concurrent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class RSThreadLocalSecureRandomTest {

  @Test
  public void testWorks() {
    assertNotNull(RSThreadLocalSecureRandom.current());
  }

  @Test
  public void testNewFastSecureRandom() {
    assertNotNull(RSThreadLocalSecureRandom.newFastSecureRandom(Arrays.asList("   ",
        "ThisIsDefinitelyNotARealAlgorithm", "NativePRNGNonBlocking", "SHA1PRNG")));
    assertNotNull(RSThreadLocalSecureRandom.newFastSecureRandom(Collections.emptyList()));
    assertNotNull(RSThreadLocalSecureRandom.newFastSecureRandom(Arrays.asList("???")));
  }

}

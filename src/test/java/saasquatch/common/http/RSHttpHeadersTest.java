package saasquatch.common.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import com.google.common.collect.ImmutableSet;

public class RSHttpHeadersTest {

  @Test
  public void testBasicAuth() {
    assertEquals("Basic QWxhZGRpbjpPcGVuU2VzYW1l", RSHttpHeaders.basicAuth("Aladdin", "OpenSesame"));
    {
      final Map.Entry<String, String> userPass =
          RSHttpHeaders.getBasicAuth("Basic QWxhZGRpbjpPcGVuU2VzYW1l").get();
      assertEquals("Aladdin", userPass.getKey());
      assertEquals("OpenSesame", userPass.getValue());
    }
    {
      final Map.Entry<String, String> userPass =
          RSHttpHeaders.getBasicAuth("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==").get();
      assertEquals("Aladdin", userPass.getKey());
      assertEquals("open sesame", userPass.getValue());
    }
    {
      final Map.Entry<String, String> userPass =
          RSHttpHeaders.getBasicAuth("Basic QWxhZGRpbjpvcGVuOnNlc2FtZQ==").get();
      assertEquals("Aladdin", userPass.getKey());
      assertEquals("open:sesame", userPass.getValue());
    }

    assertFalse(RSHttpHeaders.getBasicAuth("QWxhZGRpbjpPcGVuU2VzYW1l").isPresent(),
        "The header has to start with 'Basic '");
    assertFalse(RSHttpHeaders.getBasicAuth("basic QWxhZGRpbjpPcGVuU2VzYW1l").isPresent(),
        "'Basic' should be case sensitive");
  }

  @Test
  public void testInvalidBase64() {
    final String invalidB64 = "!@#$";
    assertThrows(IllegalArgumentException.class, () -> Base64.getDecoder().decode(invalidB64),
        invalidB64 + " should be invalid");
    assertFalse(RSHttpHeaders.getBasicAuth("Basic " + invalidB64).isPresent());
  }

  @Test
  public void testInvalidBasicAuth() {
    final String noColon = "Basic " + Base64.getEncoder().encodeToString("abcde".getBytes(UTF_8));
    assertFalse(RSHttpHeaders.getBasicAuth(noColon).isPresent());
  }

  @Test
  public void testGeneratingBasicAuthWithNull() {
    // No exception should be thrown
    final String emptyBasic = "Basic Og==";
    assertEquals(emptyBasic, RSHttpHeaders.basicAuth("", ""));
    assertEquals(emptyBasic, RSHttpHeaders.basicAuth("", null));
    assertEquals(emptyBasic, RSHttpHeaders.basicAuth(null, ""));
    assertEquals(emptyBasic, RSHttpHeaders.basicAuth(null, null));
  }

  @Test
  public void testBearerAuth() {
    assertFalse(RSHttpHeaders.getBearerAuth("QWxhZGRpbjpPcGVuU2VzYW1l").isPresent(),
        "The header has to start with 'Bearer '");
    assertFalse(RSHttpHeaders.getBearerAuth("bearer abcde").isPresent(),
        "'Bearer' should be case sensitive");
    for (int i = 0; i < 1024; i++) {
      assertTrue(RSHttpHeaders.getBearerAuth("Bearer " + RandomStringUtils.random(64)).isPresent());
    }
  }

  @Test
  public void testGetAcceptEncodings() {
    assertEquals(Collections.emptySet(), RSHttpHeaders.getAcceptedEncodings((String) null));
    assertEquals(Collections.emptySet(), RSHttpHeaders.getAcceptedEncodings((Set<String>) null));
    assertEquals(Collections.emptySet(),
        RSHttpHeaders.getAcceptedEncodings(Collections.emptyList()));
    assertEquals(ImmutableSet.of("br", "gzip"), RSHttpHeaders.getAcceptedEncodings("br, gzip"));
    assertEquals(ImmutableSet.of("br", "gzip"),
        RSHttpHeaders.getAcceptedEncodings("br;q=0.8, gzip,BR,*,iDENtity,,"));
    assertEquals(ImmutableSet.of("br", "gzip", "x-gzip", "foobar"),
        RSHttpHeaders.getAcceptedEncodings(
            Arrays.asList("br, gzip,BR,iDENtity", "identity", "x-gzip,fooBAR")));
  }

}

package saasquatch.common.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Base64;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class RSHttpUtilsTests {

  @Test
  public void testBasicAuth() {
    assertEquals("Basic QWxhZGRpbjpPcGVuU2VzYW1l", RSHttpUtils.basicAuth("Aladdin", "OpenSesame"));
    {
      final Map.Entry<String, String> userPass =
          RSHttpUtils.getBasicAuth("Basic QWxhZGRpbjpPcGVuU2VzYW1l").get();
      assertEquals("Aladdin", userPass.getKey());
      assertEquals("OpenSesame", userPass.getValue());
    }
    {
      final Map.Entry<String, String> userPass =
          RSHttpUtils.getBasicAuth("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==").get();
      assertEquals("Aladdin", userPass.getKey());
      assertEquals("open sesame", userPass.getValue());
    }
    {
      final Map.Entry<String, String> userPass =
          RSHttpUtils.getBasicAuth("Basic QWxhZGRpbjpvcGVuOnNlc2FtZQ==").get();
      assertEquals("Aladdin", userPass.getKey());
      assertEquals("open:sesame", userPass.getValue());
    }

    assertFalse("The header has to start with 'Basic '",
        RSHttpUtils.getBasicAuth("QWxhZGRpbjpPcGVuU2VzYW1l").isPresent());
    assertFalse("'Basic' should be case sensitive",
        RSHttpUtils.getBasicAuth("basic QWxhZGRpbjpPcGVuU2VzYW1l").isPresent());
  }

  @Test
  public void testInvalidBase64() {
    final String invalidB64 = "!@#$";
    try {
      Base64.getDecoder().decode(invalidB64);
      fail(invalidB64 + " should be invalid");
    } catch (IllegalArgumentException expected) {
      // ignore
    }
    assertFalse(RSHttpUtils.getBasicAuth("Basic " + invalidB64).isPresent());
  }

  @Test
  public void testBearerAuth() {
    assertFalse("The header has to start with 'Bearer '",
        RSHttpUtils.getBearerAuth("QWxhZGRpbjpPcGVuU2VzYW1l").isPresent());
    assertFalse("'Bearer' should be case sensitive",
        RSHttpUtils.getBearerAuth("bearer abcde").isPresent());
    for (int i = 0; i < 1024; i++) {
      assertTrue(RSHttpUtils.getBearerAuth("Bearer " + RandomStringUtils.random(64)).isPresent());
    }
  }

}

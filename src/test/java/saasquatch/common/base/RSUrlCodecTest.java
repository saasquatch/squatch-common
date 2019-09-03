package saasquatch.common.base;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntPredicate;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;

public class RSUrlCodecTest {

  @Test
  public void testRandomAlphanumeric() {
    for (int i = 0; i < 100; i++) {
      final String alhpanum = RandomStringUtils.randomAlphanumeric(100);
      assertEquals(alhpanum, RSUrlCodec.encodeStandard(alhpanum));
    }
  }

  @Test
  public void testReserved() throws Exception {
    final String reserved = "! # $ & ' ( ) * + , / : ; = ? @ [ ]";
    final String expected = "%21%20%23%20%24%20%26%20%27%20%28%20%29%20%2A%20%2B%20%2C%20"
        + "%2F%20%3A%20%3B%20%3D%20%3F%20%40%20%5B%20%5D";
    assertEquals(expected, RSUrlCodec.encodeStandard(reserved));
    assertEquals(reserved, RSUrlCodec.decode(expected));
    assertEquals(reserved, RSUrlCodec.decodeLenient(expected));
    assertEquals(reserved, URLDecoder.decode(expected, UTF_8.name()));
  }

  @Test
  public void testRandom() throws Exception {
    for (int i = 0; i < 100; i++) {
      final String original = RandomStringUtils.random(1024);
      final String randomSafeChars = RandomStringUtils.randomAlphanumeric(12);
      final IntPredicate pred = c -> randomSafeChars.indexOf(c) >= 0;
      assertEquals(original, RSUrlCodec.decode(RSUrlCodec.encodeStandard(original)));
      assertEquals(original, RSUrlCodec.decode(RSUrlCodec.encode(original, pred, false)));
      assertEquals(original, RSUrlCodec.decode(RSUrlCodec.encode(original, pred, true)));
      assertEquals(original, RSUrlCodec.decodeLenient(RSUrlCodec.encodeStandard(original)));
      assertEquals(original, RSUrlCodec.decodeLenient(RSUrlCodec.encode(original, pred, false)));
      assertEquals(original, RSUrlCodec.decodeLenient(RSUrlCodec.encode(original, pred, true)));
      assertEquals(original,
          URLDecoder.decode(RSUrlCodec.encode(original, pred, false), UTF_8.name()));
      assertEquals(original,
          URLDecoder.decode(RSUrlCodec.encode(original, pred, true), UTF_8.name()));
    }
  }

  @Test
  public void testIndividual() {
    final Map<String, String> reservedMappings = ImmutableMap.<String, String>builder()
        .put("!", "%21").put("#", "%23").put("$", "%24").put("&", "%26").put("'", "%27")
        .put("(", "%28").put(")", "%29").put("*", "%2A").put("+", "%2B").put(",", "%2C")
        .put("/", "%2F").put(":", "%3A").put(";", "%3B").put("=", "%3D").put("?", "%3F")
        .put("@", "%40").put("[", "%5B").put("]", "%5D").build();
    reservedMappings.forEach((k, v) -> {
      assertEquals(v, RSUrlCodec.encodeStandard(k));
    });
    reservedMappings.forEach((k, v) -> {
      assertEquals(k, RSUrlCodec.decode(v));
      assertEquals(k, RSUrlCodec.decodeLenient(v));
      try {
        assertEquals(k, URLDecoder.decode(v, UTF_8.name()));
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError(e);
      }
    });
  }

  @Test
  public void testRealHtml() throws Exception {
    final String original =
        "<h2><span class=\"mw-headline\" id=\"Percent-encoding_in_a_URI\">Percent-encoding in a URI</span><span class=\"mw-editsection\"><span class=\"mw-editsection-bracket\">[</span><a href=\"/w/index.php?title=Percent-encoding&amp;action=edit&amp;section=1\" title=\"Edit section: Percent-encoding in a URI\">edit</a><span class=\"mw-editsection-bracket\">]</span></span></h2>\r\n"
            + "<h3><span class=\"mw-headline\" id=\"Types_of_URI_characters\">Types of URI characters</span><span class=\"mw-editsection\"><span class=\"mw-editsection-bracket\">[</span><a href=\"/w/index.php?title=Percent-encoding&amp;action=edit&amp;section=2\" title=\"Edit section: Types of URI characters\">edit</a><span class=\"mw-editsection-bracket\">]</span></span></h3>\r\n"
            + "<p>The characters allowed in a URI are either <i>reserved</i> or <i>unreserved</i> (or a percent character as part of a percent-encoding). <i>Reserved</i> characters are those characters that sometimes have special meaning. For example, <a href=\"/wiki/Forward_slash\" class=\"mw-redirect\" title=\"Forward slash\">forward slash</a> characters are used to separate different parts of a URL (or more generally, a URI). <i>Unreserved</i> characters have no such meanings. Using percent-encoding, reserved characters are represented using special character sequences. The sets of reserved and unreserved characters and the circumstances under which certain reserved characters have special meaning have changed slightly with each revision of specifications that govern URIs and URI schemes.</p>";
    final String encoded =
        "%3Ch2%3E%3Cspan%20class%3D%22mw-headline%22%20id%3D%22Percent-encoding_in_a_URI%22%3EPercent-encoding%20in%20a%20URI%3C%2Fspan%3E%3Cspan%20class%3D%22mw-editsection%22%3E%3Cspan%20class%3D%22mw-editsection-bracket%22%3E%5B%3C%2Fspan%3E%3Ca%20href%3D%22%2Fw%2Findex.php%3Ftitle%3DPercent-encoding%26amp%3Baction%3Dedit%26amp%3Bsection%3D1%22%20title%3D%22Edit%20section%3A%20Percent-encoding%20in%20a%20URI%22%3Eedit%3C%2Fa%3E%3Cspan%20class%3D%22mw-editsection-bracket%22%3E%5D%3C%2Fspan%3E%3C%2Fspan%3E%3C%2Fh2%3E%0D%0A%3Ch3%3E%3Cspan%20class%3D%22mw-headline%22%20id%3D%22Types_of_URI_characters%22%3ETypes%20of%20URI%20characters%3C%2Fspan%3E%3Cspan%20class%3D%22mw-editsection%22%3E%3Cspan%20class%3D%22mw-editsection-bracket%22%3E%5B%3C%2Fspan%3E%3Ca%20href%3D%22%2Fw%2Findex.php%3Ftitle%3DPercent-encoding%26amp%3Baction%3Dedit%26amp%3Bsection%3D2%22%20title%3D%22Edit%20section%3A%20Types%20of%20URI%20characters%22%3Eedit%3C%2Fa%3E%3Cspan%20class%3D%22mw-editsection-bracket%22%3E%5D%3C%2Fspan%3E%3C%2Fspan%3E%3C%2Fh3%3E%0D%0A%3Cp%3EThe%20characters%20allowed%20in%20a%20URI%20are%20either%20%3Ci%3Ereserved%3C%2Fi%3E%20or%20%3Ci%3Eunreserved%3C%2Fi%3E%20%28or%20a%20percent%20character%20as%20part%20of%20a%20percent-encoding%29.%20%3Ci%3EReserved%3C%2Fi%3E%20characters%20are%20those%20characters%20that%20sometimes%20have%20special%20meaning.%20For%20example%2C%20%3Ca%20href%3D%22%2Fwiki%2FForward_slash%22%20class%3D%22mw-redirect%22%20title%3D%22Forward%20slash%22%3Eforward%20slash%3C%2Fa%3E%20characters%20are%20used%20to%20separate%20different%20parts%20of%20a%20URL%20%28or%20more%20generally%2C%20a%20URI%29.%20%3Ci%3EUnreserved%3C%2Fi%3E%20characters%20have%20no%20such%20meanings.%20Using%20percent-encoding%2C%20reserved%20characters%20are%20represented%20using%20special%20character%20sequences.%20The%20sets%20of%20reserved%20and%20unreserved%20characters%20and%20the%20circumstances%20under%20which%20certain%20reserved%20characters%20have%20special%20meaning%20have%20changed%20slightly%20with%20each%20revision%20of%20specifications%20that%20govern%20URIs%20and%20URI%20schemes.%3C%2Fp%3E";
    assertEquals(encoded, RSUrlCodec.encodeStandard(original));
    assertEquals(original, RSUrlCodec.decode(encoded));
    assertEquals(original, RSUrlCodec.decodeLenient(encoded));
    assertEquals(original, URLDecoder.decode(encoded, UTF_8.name()));
  }

  @Test
  public void testFormCompatibility() throws Exception {
    for (int i = 0; i < 1024; i++) {
      final String fakeString = RandomStringUtils.random(1024);
      final String ourEncoded = RSUrlCodec.encodeForm(fakeString);
      final String javaEncoded = URLEncoder.encode(fakeString, UTF_8.name());
      assertEquals(javaEncoded, ourEncoded);
    }
  }

  @Test
  public void testCustomEncodingRules() {
    // Encode nothing
    for (int i = 0; i < 1024; i++) {
      final String fakeString = RandomStringUtils.randomAlphanumeric(1024);
      final String encoded = RSUrlCodec.encode(fakeString, _i -> true, false);
      assertEquals(fakeString, encoded);
    }

    // Test some custom rules
    {
      final String toEncode =
          "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">";
      assertEquals(
          "%3C%68%74%6D%6C+%78%6D%6C%6E%73%3D%22%68%74%74%70%3A%2F%2F%77%77%77%2E%77%33%2E%6F%72%67%2F%31%39%39%39%2F%78%68%74%6D%6C%22+%78%6D%6C%3A%6C%61%6E%67%3D%22%65%6E%22+%6C%61%6E%67%3D%22%65%6E%22%3E",
          RSUrlCodec.encode(toEncode, _i -> false, true));
      assertEquals(
          "%3C%68%74%6D%6C%20%78%6D%6C%6E%73%3D%22%68%74%74%70%3A%2F%2F%77%77%77%2E%77%33%2E%6F%72%67%2F%31%39%39%39%2F%78%68%74%6D%6C%22%20%78%6D%6C%3A%6C%61%6E%67%3D%22%65%6E%22%20%6C%61%6E%67%3D%22%65%6E%22%3E",
          RSUrlCodec.encode(toEncode, _i -> false, false));
      assertEquals(
          "<%68%74%6D%6C%20%78%6D%6C%6E%73=%22%68%74%74%70:%2F%2F%77%77%77%2E%77%33%2E%6F%72%67%2F%31%39%39%39%2F%78%68%74%6D%6C%22%20%78%6D%6C:%6C%61%6E%67=%22%65%6E%22%20%6C%61%6E%67=%22%65%6E%22>",
          RSUrlCodec.encode(toEncode, _i -> _i == '=' || _i == ':' || _i == '<' || _i == '>', false));
    }

    // Test rules that don't make sense
    for (int i = 0; i < 1024; i++) {
      final String toEncode = RandomStringUtils.randomAlphanumeric(1024);
      final IntPredicate randomPred = _i -> ThreadLocalRandom.current().nextBoolean();
      assertNotEquals(
          RSUrlCodec.encode(toEncode, randomPred, false),
          RSUrlCodec.encode(toEncode, randomPred, false));
    }
  }

  @Test
  public void testEncodingSpace() {
    assertEquals(" ", RSUrlCodec.encode(" ", anything -> true, true));
    assertEquals("+", RSUrlCodec.encode(" ", anything -> false, true));
    assertEquals("%20", RSUrlCodec.encode(" ", anything -> false, false));
  }

  @Test
  public void testDecodingSpace() {
    assertEquals(" ", RSUrlCodec.decode("+", true));
    assertEquals("+", RSUrlCodec.decode("+", false));
  }

  @Test
  public void testInvalid() {
    try {
      RSUrlCodec.decode("%4%44");
      fail();
    } catch (IllegalArgumentException expected) {}
    assertEquals("%4D", RSUrlCodec.decodeLenient("%4%44"));
    try {
      RSUrlCodec.decode("%4.%44");
      fail();
    } catch (IllegalArgumentException expected) {}
    assertEquals("%4.D", RSUrlCodec.decodeLenient("%4.%44"));
    try {
      RSUrlCodec.decode("%44%4");
      fail();
    } catch (IllegalArgumentException expected) {}
    assertEquals("D%4", RSUrlCodec.decodeLenient("%44%4"));
  }

}

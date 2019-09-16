package saasquatch.common.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import saasquatch.common.collect.RSCollectors;

public final class RSHttpHeaders {

  private static final String BASIC_PREFIX = "Basic ";
  private static final String BEARER_PREFIX = "Bearer ";

  private RSHttpHeaders() {}

  /**
   * Parses basic authorization header for username/password
   *
   * @return A String Pair containing the username/password of the Basic Auth header or null if the
   *         header is missing or incorrectly formatted
   */
  public static Optional<Map.Entry<String, String>> getBasicAuth(
      @Nullable String authorizationHeader) {
    return Optional.ofNullable(authorizationHeader)
        .filter(s -> s.startsWith(BASIC_PREFIX))
        .map(s -> s.substring(BASIC_PREFIX.length()))
        .map(s -> {
          try {
            return Base64.getDecoder().decode(s);
          } catch (IllegalArgumentException e) {
            return null; // invalid base64
          }
        })
        .map(bytes -> new String(bytes, UTF_8))
        .map(s -> s.split(":", 2))
        .filter(tokens -> tokens.length == 2)
        .map(tokens -> new SimpleImmutableEntry<>(tokens[0], tokens[1]));
  }

  public static Optional<String> getBearerAuth(@Nullable String authorizationHeader) {
    return Optional.ofNullable(authorizationHeader)
        .filter(s -> s.startsWith(BEARER_PREFIX))
        .map(s -> s.substring(BEARER_PREFIX.length()));
  }

  /**
   * Generate a Basic Authorization header
   */
  public static String basicAuth(@Nullable String username, @Nullable String password) {
    final byte[] userPassBytes = Stream.of(username, password)
        .map(s -> s == null ? "" : s)
        .collect(Collectors.joining(":"))
        .getBytes(UTF_8);
    return BASIC_PREFIX + Base64.getEncoder().encodeToString(userPassBytes);
  }

  /**
   * Get a list of content encodings from Accept-Encoding header
   */
  public static Set<String> getAcceptedEncodings(@Nullable String acceptEncoding) {
    if (acceptEncoding == null)
      return Collections.emptySet();
    return Arrays.stream(acceptEncoding.split(","))
        .map(s -> {
          final int idx = s.indexOf(';');
          return idx < 0 ? s : s.substring(0, idx);
        })
        .map(String::trim)
        .filter(s -> s.length() > 0)
        .map(String::toLowerCase)
        .filter(s -> !"identity".equals(s))
        .filter(s -> !"*".equals(s))
        .collect(RSCollectors.toUnmodifiableSet());
  }

  /**
   * Get a list of content encodings from Accept-Encoding header
   */
  public static Set<String> getAcceptedEncodings(@Nullable Collection<String> acceptEncoding) {
    if (acceptEncoding == null || acceptEncoding.isEmpty())
      return Collections.emptySet();
    return acceptEncoding.stream()
        .map(RSHttpHeaders::getAcceptedEncodings)
        .flatMap(Collection::stream)
        .collect(RSCollectors.toUnmodifiableSet());
  }

}

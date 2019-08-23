package saasquatch.common.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RSJacksonTest {

  @Test
  public void testArrayNodeCreation() {
    final List<JsonNode> jsonNodes = generateJsonNodesList();
    final ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
    arrayNode.addAll(jsonNodes);
    assertEquals(arrayNode, RSJackson.arrayNodeOf(jsonNodes.toArray(new JsonNode[0])));
  }

  @Test
  public void testArrayNodeCollector() {
    final List<JsonNode> jsonNodes = generateJsonNodesList();
    final ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
    arrayNode.addAll(jsonNodes);
    final ArrayNode collect = jsonNodes.stream()
        .collect(RSJackson.toArrayNode());
    assertEquals(arrayNode, collect);
  }

  @Test
  public void testNullAndEmpty() {
    doTestShouldBeNullAndEmpty(null);
    doTestShouldBeNullAndEmpty(JsonNodeFactory.instance.nullNode());
    doTestShouldBeNullAndEmpty(MissingNode.getInstance());
    doTestShouldNotBeNullOrEmpty(JsonNodeFactory.instance.objectNode().put("foo", "bar"));
    doTestShouldNotBeNullOrEmpty(RSJackson.arrayNodeOf(JsonNodeFactory.instance.textNode("foo")));
    doTestShouldNotBeNullOrEmpty(JsonNodeFactory.instance.numberNode(0));

    final ObjectNode emptyObject = JsonNodeFactory.instance.objectNode();
    assertFalse(RSJackson.isNull(emptyObject));
    assertTrue(RSJackson.isEmpty(emptyObject));

    final ArrayNode emptyArray = JsonNodeFactory.instance.arrayNode();
    assertFalse(RSJackson.isNull(emptyArray));
    assertTrue(RSJackson.isEmpty(emptyArray));
  }

  private static List<JsonNode> generateJsonNodesList() {
    final List<JsonNode> jsonNodes = new ArrayList<>();
    ThreadLocalRandom.current().ints(64)
        .mapToObj(JsonNodeFactory.instance::numberNode)
        .forEach(jsonNodes::add);
    Stream.generate(() -> RandomStringUtils.randomAlphanumeric(16))
        .limit(64)
        .map(JsonNodeFactory.instance::textNode)
        .forEach(jsonNodes::add);
    Stream.generate(() -> JsonNodeFactory.instance.objectNode()
        .put(RandomStringUtils.randomAlphanumeric(16), RandomStringUtils.randomAlphanumeric(16)))
        .limit(64)
        .forEach(jsonNodes::add);
    return jsonNodes;
  }

  private static void doTestShouldBeNullAndEmpty(@Nullable JsonNode j) {
    assertTrue(RSJackson.isNull(j));
    assertFalse(RSJackson.nonNull(j));
    assertTrue(RSJackson.isEmpty(j));
    assertFalse(RSJackson.nonEmpty(j));
  }

  private static void doTestShouldNotBeNullOrEmpty(@Nullable JsonNode j) {
    assertFalse(RSJackson.isNull(j));
    assertTrue(RSJackson.nonNull(j));
    assertFalse(RSJackson.isEmpty(j));
    assertTrue(RSJackson.nonEmpty(j));
  }

}

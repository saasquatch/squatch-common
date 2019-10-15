package com.saasquatch.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RSJacksonTest {

  private final ObjectMapper mapper = new ObjectMapper();

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
    {
      final ArrayNode collect = jsonNodes.stream()
          .collect(RSJackson.toArrayNode());
      assertEquals(arrayNode, collect);
    }
    {
      final ArrayNode collect = jsonNodes.parallelStream()
          .collect(RSJackson.toArrayNode());
      assertEquals(arrayNode, collect);
    }
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

  @Test
  public void testMutateValueNode() {
    final String baseDir = "testMutateValueNodes/";
    // Test MissingNode
    assertTrue(RSJackson.mutateValueNodes(MissingNode.getInstance(), j -> j).isMissingNode());
    // identity function
    assertEquals(loadJson(baseDir + "result1.json"),
        RSJackson.mutateValueNodes(loadJson(baseDir + "test1.json"), j -> j));
    // Tuncate every String
    assertEquals(loadJson(baseDir + "result2.json"),
        RSJackson.mutateValueNodes(loadJson(baseDir + "test2.json"), j -> {
          if (!j.isTextual()) return j;
          final String textValue = j.textValue();
          if (textValue.length() <= 5) return j;
          return JsonNodeFactory.instance.textNode(textValue.substring(0, 5));
        }));
    // Multiple every integer by 2
    assertEquals(loadJson(baseDir + "result3.json"),
        RSJackson.mutateValueNodes(loadJson(baseDir + "test3.json"), j -> {
          if (!j.isIntegralNumber()) return j;
          final int intValue = j.intValue();
          return JsonNodeFactory.instance.numberNode(intValue * 2);
        }));
  }

  @Test
  public void testRenameField() {
    final ObjectNode obj = JsonNodeFactory.instance.objectNode()
        .put("foo", "bar");
    {
      final ObjectNode renamed = RSJackson.renameField(obj, "fieldThatDoesNotExist", "bar");
      assertEquals(obj, renamed);
    }
    {
      final ObjectNode renamed = RSJackson.renameField(obj, "foo", "bar");
      assertEquals(JsonNodeFactory.instance.objectNode().put("bar", "bar"), renamed);
    }
  }

  @Test
  public void testShallowMerge() {
    final ObjectNode obj1 = JsonNodeFactory.instance.objectNode()
        .put("one", 1).put("two", "two").put("three", 1);
    final ObjectNode obj2 = JsonNodeFactory.instance.objectNode()
        .put("two", 2).put("three", "three");
    final ObjectNode obj3 = JsonNodeFactory.instance.objectNode()
        .put("three", 3);

    final ObjectNode expectedResult = JsonNodeFactory.instance.objectNode()
        .put("one", 1).put("two", 2).put("three", 3);
    assertEquals(expectedResult, RSJackson.shallowMerge(obj1, obj2, obj3));
  }

  private JsonNode loadJson(String fileName) {
    try (InputStream in = this.getClass().getResourceAsStream(fileName)) {
      return mapper.readTree(in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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

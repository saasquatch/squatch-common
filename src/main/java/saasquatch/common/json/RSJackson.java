package saasquatch.common.json;

import java.util.function.Function;
import java.util.stream.Collector;
import javax.annotation.Nonnull;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/**
 * Utilities for Jackson.
 *
 * @author sli
 */
public class RSJackson {

  /**
   * A {@link Collector} that collects {@link JsonNode}s into an {@link ArrayNode}
   */
  public static <T extends JsonNode> Collector<T, ?, ArrayNode> toArrayNode() {
    return Collector.of(JsonNodeFactory.instance::arrayNode, ArrayNode::add,
        (a1, a2) -> {
          a1.addAll(a2);
          return a1;
        });
  }

  /**
   * Create an {@link ArrayNode} with the given {@link JsonNode}s.
   */
  public static ArrayNode arrayNodeOf(JsonNode... nodes) {
    final ArrayNode result = JsonNodeFactory.instance.arrayNode();
    for (JsonNode node : nodes) {
      result.add(node);
    }
    return result;
  }

  /**
   * Mutate each value/leaf node. Note that this method does not modify the original
   * {@link JsonNode}.
   *
   * @param json
   * @param mutation The actual mutation. The caller can assume that the input of the mutation is a
   *        value node.
   * @return The mutated {@link JsonNode}
   */
  public static JsonNode mutateValueNodes(@Nonnull final JsonNode json,
      @Nonnull final Function<ValueNode, JsonNode> mutation) {
    if (json.isMissingNode()) {
      return json;
    } else if (json.isArray()) {
      final ArrayNode newNode = JsonNodeFactory.instance.arrayNode();
      for (JsonNode v : json) {
        newNode.add(mutateValueNodes(v, mutation));
      }
      return newNode;
    } else if (json.isValueNode()) {
      return mutation.apply((ValueNode) json);
    } else {
      final ObjectNode newNode = JsonNodeFactory.instance.objectNode();
      json.fields().forEachRemaining(e -> {
        final String k = e.getKey();
        final JsonNode v = e.getValue();
        newNode.set(k, mutateValueNodes(v, mutation));
      });
      return newNode;
    }
  }

  public static boolean isNull(JsonNode j) {
    return j == null || j.isMissingNode() || j.isNull();
  }

  public static boolean nonNull(JsonNode j) {
    return !isNull(j);
  }

  public static boolean isEmpty(JsonNode j) {
    if (isNull(j))
      return true;
    if (j.isValueNode())
      return false;
    return j.size() == 0;
  }

  public static boolean nonEmpty(JsonNode j) {
    return !isEmpty(j);
  }

  /**
   * Rename a field in an {@link ObjectNode}. Note that this method does not modify the original
   * json and returns a copy.
   */
  public static ObjectNode renameField(@Nonnull final ObjectNode json,
      @Nonnull final String oldName, @Nonnull final String newName) {
    final ObjectNode result = json.deepCopy();
    result.set(newName, result.path(oldName));
    result.remove(oldName);
    return result;
  }

  /**
   * Shallow merge an array of objects into one new object
   */
  public static ObjectNode shallowMerge(@Nonnull JsonNode... jsons) {
    final ObjectNode result = JsonNodeFactory.instance.objectNode();
    for (final JsonNode json : jsons) {
      json.fields().forEachRemaining(entry -> {
        result.set(entry.getKey(), entry.getValue());
      });
    }
    return result;
  }

}

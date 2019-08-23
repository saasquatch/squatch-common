package saasquatch.common.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class RSCollectorsTest {

  @Test
  public void testEmptyList() {
    final List<Object> collect = Stream.empty()
        .collect(RSCollectors.toUnmodifiableList());
    assertTrue("We should be getting the singleton emptyList", collect == Collections.emptyList());
  }

  @Test
  public void testEmptySet() {
    {
      final Set<Object> collect = Stream.empty()
          .collect(RSCollectors.toUnmodifiableSet());
      assertTrue("We should be getting the singleton emptySet", collect == Collections.emptySet());
    }
    {
      final Set<Object> collect = Stream.empty()
          .collect(RSCollectors.toUnmodifiableSet(LinkedHashSet::new));
      assertTrue("We should be getting the singleton emptySet even with a custom Set",
          collect == Collections.emptySet());
    }
  }

  @Test
  public void testEmptySortedSet() {
    final Set<Object> collect = Stream.empty()
        .collect(RSCollectors.toUnmodifiableSortedSet(TreeSet::new));
    assertTrue("We should be getting the singleton emptySortedSet",
        collect == Collections.emptySortedSet());
  }

  @Test
  public void testEmptyNavigableSet() {
    final Set<Object> collect = Stream.empty()
        .collect(RSCollectors.toUnmodifiableNavigableSet(TreeSet::new));
    assertTrue("We should be getting the singleton emptyNavigableSet",
        collect == Collections.emptyNavigableSet());
  }

  @Test
  public void testEmptyMap() {
    {
      final Map<Object, Object> collect = Stream.empty()
          .collect(RSCollectors.toUnmodifiableMap(Function.identity(), Function.identity()));
      assertTrue("We should be getting the singleton emptyMap", collect == Collections.emptyMap());
    }
    {
      final Map<Object, Object> collect = Stream.empty()
          .collect(RSCollectors.toUnmodifiableMap(Function.identity(), Function.identity(),
              RSCollectors.throwingMerger(), LinkedHashMap::new));
      assertTrue("We should be getting the singleton emptyMap even with a custom Set",
          collect == Collections.emptyMap());
    }
  }

  @Test
  public void testEmptySortedMap() {
    final Map<Object, Object> collect = Stream.empty()
        .collect(RSCollectors.toUnmodifiableSortedMap(Function.identity(), Function.identity(),
            RSCollectors.throwingMerger(), TreeMap::new));
    assertTrue("We should be getting the singleton emptySortedMap",
        collect == Collections.emptySortedMap());
  }

  @Test
  public void testEmptyNavigableMap() {
    final Map<Object, Object> collect = Stream.empty()
        .collect(RSCollectors.toUnmodifiableNavigableMap(Function.identity(), Function.identity(),
            RSCollectors.throwingMerger(), TreeMap::new));
    assertTrue("We should be getting the singleton emptyNavigableMap",
        collect == Collections.emptyNavigableMap());
  }

  @Test
  public void testListBasic() {
    final List<String> original = Stream.generate(() -> RandomStringUtils.randomAlphanumeric(128))
        .limit(128)
        .collect(Collectors.toList());
    final List<String> collected = original.stream()
        .collect(RSCollectors.toUnmodifiableList());
    assertEquals(original, collected);
    try {
      collected.clear();
      fail("The result should be unmodifiable");
    } catch (UnsupportedOperationException expected) {}
  }

  @Test
  public void testSetBasic() {
    final Set<String> original = Stream.generate(() -> RandomStringUtils.randomAlphanumeric(128))
        .limit(128)
        .collect(Collectors.toSet());
    final Set<String> collected1 = original.stream()
        .collect(RSCollectors.toUnmodifiableSet());
    final Set<String> collected2 = original.stream()
        .collect(RSCollectors.toUnmodifiableSet(TreeSet::new));
    for (Set<String> collected : Arrays.asList(collected1, collected2)) {
      assertEquals(original, collected);
      try {
        collected.clear();
        fail("The result should be unmodifiable");
      } catch (UnsupportedOperationException expected) {}
    }
  }

  @Test
  public void testMapBasic() {
    final Map<String, String> original =
        Stream.generate(() -> RandomStringUtils.randomAlphanumeric(128)).limit(128)
            .collect(Collectors.toMap(Function.identity(), Function.identity()));
    final Map<String, String> collected1 = original.entrySet().stream()
        .collect(RSCollectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    final Map<String, String> collected2 = original.entrySet().stream()
        .collect(RSCollectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue,
            RSCollectors.throwingMerger(), TreeMap::new));
    for (Map<String, String> collected : Arrays.asList(collected1, collected2)) {
      assertEquals(original, collected);
      try {
        collected.clear();
        fail("The result should be unmodifiable");
      } catch (UnsupportedOperationException expected) {}
    }
  }

  @Test
  public void testThrowingMerger() {
    try {
      Stream.of(1, 1)
          .collect(RSCollectors.toUnmodifiableMap(Function.identity(), Function.identity(),
              RSCollectors.throwingMerger(), ConcurrentHashMap::new));
      fail("We should be getting a merger exception");
    } catch (IllegalStateException expected) {}
  }

}

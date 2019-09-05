package saasquatch.common.collect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public class RSCollectorsTest {

  @Test
  public void testEmptyList() {
    final List<Object> collect = Stream.empty().collect(RSCollectors.toUnmodifiableList());
    assertTrue(collect == Collections.emptyList(), "We should be getting the singleton emptyList");
  }

  @Test
  public void testSingletonList() {
    final List<Object> collect = Stream.of(1).collect(RSCollectors.toUnmodifiableList());
    assertEquals("SingletonList", collect.getClass().getSimpleName(),
        "We should be getting a SingletonList");
  }

  @Test
  public void testEmptySet() {
    {
      final Set<Object> collect = Stream.empty().collect(RSCollectors.toUnmodifiableSet());
      assertTrue(collect == Collections.emptySet(), "We should be getting the singleton emptySet");
    }
    {
      final Set<Object> collect = Stream.empty()
          .collect(RSCollectors.toUnmodifiableSet(LinkedHashSet::new));
      assertTrue(collect == Collections.emptySet(),
          "We should be getting the singleton emptySet even with a custom Set");
    }
  }

  @Test
  public void testSingletonSet() {
    {
      final Set<Object> collect = Stream.of(1)
          .collect(RSCollectors.toUnmodifiableSet());
      assertEquals("SingletonSet", collect.getClass().getSimpleName(),
          "We should be getting a SingletonSet");
    }
    {
      final Set<Object> collect = Stream.of(1)
          .collect(RSCollectors.toUnmodifiableSet(LinkedHashSet::new));
      assertEquals("SingletonSet", collect.getClass().getSimpleName(),
          "We should be getting a SingletonSet even with a custom Set");
    }
  }


  @Test
  public void testSortedSet() {
    final SortedSet<Integer> original = ThreadLocalRandom.current().ints(128).boxed()
        .collect(Collectors.toCollection(TreeSet::new));
    final SortedSet<Integer> collect = original.stream()
        .collect(RSCollectors.toUnmodifiableSortedSet(TreeSet::new));
    assertEquals(original, collect);
  }

  @Test
  public void testNavigableSet() {
    final NavigableSet<Integer> original = ThreadLocalRandom.current().ints(128).boxed()
        .collect(Collectors.toCollection(TreeSet::new));
    final NavigableSet<Integer> collect = original.stream()
        .collect(RSCollectors.toUnmodifiableNavigableSet(TreeSet::new));
    assertEquals(original, collect);
  }

  @Test
  public void testEmptySortedSet() {
    final Set<Object> collect = Stream.empty()
        .collect(RSCollectors.toUnmodifiableSortedSet(TreeSet::new));
    assertTrue(collect == Collections.emptySortedSet(),
        "We should be getting the singleton emptySortedSet");
  }

  @Test
  public void testEmptyNavigableSet() {
    final Set<Object> collect = Stream.empty()
        .collect(RSCollectors.toUnmodifiableNavigableSet(TreeSet::new));
    assertTrue(collect == Collections.emptyNavigableSet(),
        "We should be getting the singleton emptyNavigableSet");
  }

  @Test
  public void testEmptyMap() {
    {
      final Map<Object, Object> collect = Stream.empty()
          .collect(RSCollectors.toUnmodifiableMap(Function.identity(), Function.identity()));
      assertTrue(collect == Collections.emptyMap(), "We should be getting the singleton emptyMap");
    }
    {
      final Map<Object, Object> collect = Stream.empty()
          .collect(RSCollectors.toUnmodifiableMap(Function.identity(), Function.identity(),
              RSCollectors.throwingMerger(), LinkedHashMap::new));
      assertTrue(collect == Collections.emptyMap(),
          "We should be getting the singleton emptyMap even with a custom Map");
    }
  }

  @Test
  public void testSingletonMap() {
    {
      final Map<Object, Object> collect = Stream.of(1)
          .collect(RSCollectors.toUnmodifiableMap(Function.identity(), Function.identity()));
      assertEquals("SingletonMap", collect.getClass().getSimpleName(),
          "We should be getting a SingletonMap");
    }
    {
      final Map<Object, Object> collect = Stream.of(1)
          .collect(RSCollectors.toUnmodifiableMap(Function.identity(), Function.identity(),
              RSCollectors.throwingMerger(), LinkedHashMap::new));
      assertEquals("SingletonMap", collect.getClass().getSimpleName(),
          "We should be getting a SingletonMap even with a custom Map");
    }
  }

  @Test
  public void testSortedMap() {
    final SortedMap<Integer,Integer> original = IntStream.range(1, 128).boxed()
        .collect(
            Collectors.toMap(Function.identity(), ignored -> ThreadLocalRandom.current().nextInt(),
                RSCollectors.throwingMerger(), TreeMap::new));
    final SortedMap<Integer, Integer> collect = original.entrySet().stream()
        .collect(RSCollectors.toUnmodifiableSortedMap(Map.Entry::getKey, Map.Entry::getValue,
            RSCollectors.throwingMerger(), TreeMap::new));
    assertEquals(original, collect);
  }

  @Test
  public void testNavigableMap() {
    final NavigableMap<Integer,Integer> original = IntStream.range(1, 128).boxed()
        .collect(
            Collectors.toMap(Function.identity(), ignored -> ThreadLocalRandom.current().nextInt(),
                RSCollectors.throwingMerger(), TreeMap::new));
    final NavigableMap<Integer, Integer> collect = original.entrySet().stream()
        .collect(RSCollectors.toUnmodifiableNavigableMap(Map.Entry::getKey, Map.Entry::getValue,
            RSCollectors.throwingMerger(), TreeMap::new));
    assertEquals(original, collect);
  }

  @Test
  public void testEmptySortedMap() {
    final Map<Object, Object> collect = Stream.empty()
        .collect(RSCollectors.toUnmodifiableSortedMap(Function.identity(), Function.identity(),
            RSCollectors.throwingMerger(), TreeMap::new));
    assertTrue(collect == Collections.emptySortedMap(),
        "We should be getting the singleton emptySortedMap");
  }

  @Test
  public void testEmptyNavigableMap() {
    final Map<Object, Object> collect = Stream.empty()
        .collect(RSCollectors.toUnmodifiableNavigableMap(Function.identity(), Function.identity(),
            RSCollectors.throwingMerger(), TreeMap::new));
    assertTrue(collect == Collections.emptyNavigableMap(),
        "We should be getting the singleton emptyNavigableMap");
  }

  @Test
  public void testListBasic() {
    final List<String> original = Stream.generate(() -> RandomStringUtils.randomAlphanumeric(128))
        .limit(128)
        .collect(Collectors.toList());
    final List<String> collect = original.stream()
        .collect(RSCollectors.toUnmodifiableList());
    assertEquals(original, collect);
    try {
      collect.clear();
      fail("The result should be unmodifiable");
    } catch (UnsupportedOperationException expected) {}
  }

  @Test
  public void testSetBasic() {
    final Set<String> original = Stream.generate(() -> RandomStringUtils.randomAlphanumeric(128))
        .limit(128)
        .collect(Collectors.toSet());
    final Set<String> collect1 = original.stream()
        .collect(RSCollectors.toUnmodifiableSet());
    final Set<String> collect2 = original.stream()
        .collect(RSCollectors.toUnmodifiableSet(TreeSet::new));
    for (Set<String> collect : Arrays.asList(collect1, collect2)) {
      assertEquals(original, collect);
      try {
        collect.clear();
        fail("The result should be unmodifiable");
      } catch (UnsupportedOperationException expected) {}
    }
  }

  @Test
  public void testEnumSetBasic() {
    final Set<TimeUnit> original = EnumSet.complementOf(EnumSet.of(TimeUnit.DAYS));
    final Set<TimeUnit> collect = original.stream()
        .collect(RSCollectors.toUnmodifiableEnumSet(TimeUnit.class));
    assertEquals(original, collect);
  }

  @Test
  public void testEmptyEnumSet() {
    final Set<TimeUnit> collect =
        Stream.<TimeUnit>empty().collect(RSCollectors.toUnmodifiableEnumSet(TimeUnit.class));
    assertTrue(collect == Collections.<TimeUnit>emptySet(),
        "We should be getting the singleton emptySet");
  }

  @Test
  public void testMapBasic() {
    final Map<String, String> original =
        Stream.generate(() -> RandomStringUtils.randomAlphanumeric(128)).limit(128)
            .collect(Collectors.toMap(Function.identity(), Function.identity()));
    final Map<String, String> collect1 = original.entrySet().stream()
        .collect(RSCollectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    final Map<String, String> collect2 = original.entrySet().stream()
        .collect(RSCollectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue,
            RSCollectors.throwingMerger(), TreeMap::new));
    for (Map<String, String> collect : Arrays.asList(collect1, collect2)) {
      assertEquals(original, collect);
      try {
        collect.clear();
        fail("The result should be unmodifiable");
      } catch (UnsupportedOperationException expected) {}
    }
  }

  @Test
  public void testEnumMapBasic() {
    final Map<TimeUnit, Object> original = new EnumMap<>(TimeUnit.class);
    original.put(TimeUnit.DAYS, 1);
    original.put(TimeUnit.SECONDS, 1);
    final Map<TimeUnit, Object> collect = original.entrySet().stream()
        .collect(RSCollectors.toUnmodifiableEnumMap(Map.Entry::getKey, Map.Entry::getValue,
            TimeUnit.class));
    assertEquals(original, collect);
  }

  @Test
  public void testEmptyEnumMap() {
    final Map<TimeUnit, Object> collect = Stream.<TimeUnit>empty()
        .collect(RSCollectors.toUnmodifiableEnumMap(Function.identity(), Function.identity(),
            TimeUnit.class));
    assertTrue(collect == Collections.<TimeUnit, Object>emptyMap(),
        "We should be getting the singleton emptyMap");
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

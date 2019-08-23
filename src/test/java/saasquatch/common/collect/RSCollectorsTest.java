package saasquatch.common.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
  public void testSingletonList() {
    final List<Object> collect = Stream.of(1)
        .collect(RSCollectors.toUnmodifiableList());
    assertEquals("We should be getting a SingletonList", "SingletonList",
        collect.getClass().getSimpleName());
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
  public void testSingletonSet() {
    {
      final Set<Object> collect = Stream.of(1)
          .collect(RSCollectors.toUnmodifiableSet());
      assertEquals("We should be getting a SingletonSet", "SingletonSet",
          collect.getClass().getSimpleName());
    }
    {
      final Set<Object> collect = Stream.of(1)
          .collect(RSCollectors.toUnmodifiableSet(LinkedHashSet::new));
      assertEquals("We should be getting a SingletonSet even with a custom Set", "SingletonSet",
          collect.getClass().getSimpleName());
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
      assertTrue("We should be getting the singleton emptyMap even with a custom Map",
          collect == Collections.emptyMap());
    }
  }

  @Test
  public void testSingletonMap() {
    {
      final Map<Object, Object> collect = Stream.of(1)
          .collect(RSCollectors.toUnmodifiableMap(Function.identity(), Function.identity()));
      assertEquals("We should be getting a SingletonMap", "SingletonMap",
          collect.getClass().getSimpleName());
    }
    {
      final Map<Object, Object> collect = Stream.of(1)
          .collect(RSCollectors.toUnmodifiableMap(Function.identity(), Function.identity(),
              RSCollectors.throwingMerger(), LinkedHashMap::new));
      assertEquals("We should be getting a SingletonMap even with a custom Map", "SingletonMap",
          collect.getClass().getSimpleName());
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
    final Set<TimeUnit> collect = Stream.<TimeUnit>empty()
        .collect(RSCollectors.toUnmodifiableEnumSet(TimeUnit.class));
    assertTrue("We should be getting the singleton emptySet",
        collect == Collections.<TimeUnit>emptySet());
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
    assertTrue("We should be getting the singleton emptyMap",
        collect == Collections.<TimeUnit, Object>emptyMap());
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

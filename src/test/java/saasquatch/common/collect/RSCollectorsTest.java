package saasquatch.common.collect;

import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;
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

}

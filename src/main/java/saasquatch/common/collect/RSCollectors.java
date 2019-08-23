package saasquatch.common.collect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Better {@link Collector}s than the ones in {@link Collectors}.
 *
 * @author sli
 */
public final class RSCollectors {

  private RSCollectors() {}

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link List}.
   *
   * @see Collectors#toList()
   */
  public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
    return Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new),
        (ArrayList<T> l) -> {
          switch (l.size()) {
            case 0:
              return Collections.emptyList();
            case 1:
              return Collections.singletonList(l.get(0));
            default:
              l.trimToSize();
              return Collections.unmodifiableList(l);
          }
        });
  }

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link Set}.
   *
   * @see Collectors#toSet()
   */
  public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
    return Collectors.collectingAndThen(Collectors.toSet(), RSCollectors::unmodifiableSetFinisher);
  }

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link Set}, where you can
   * specify the underlying {@link Set}. If the result is empty or has one element, then your
   * {@link Set} will not be used.
   *
   * @see Collectors#toSet()
   */
  public static <T, S extends Set<T>> Collector<T, ?, Set<T>> toUnmodifiableSet(
      @Nonnull final Supplier<S> setSupplier) {
    return Collectors.collectingAndThen(Collectors.toCollection(setSupplier),
        RSCollectors::unmodifiableSetFinisher);
  }

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link Set} that's
   * <em>likely</em> backed by an {@link EnumSet}.
   */
  public static <T extends Enum<T>> Collector<T, ?, Set<T>> toUnmodifiableEnumSet(
      @Nonnull final Class<T> clazz) {
    return toUnmodifiableSet(() -> EnumSet.noneOf(clazz));
  }

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link SortedSet}. If the result
   * is empty, then your {@link Set} will not be used.
   */
  public static <T, S extends SortedSet<T>> Collector<T, ?, SortedSet<T>> toUnmodifiableSortedSet(
      @Nonnull final Supplier<S> sortedSetSupplier) {
    return Collectors.collectingAndThen(Collectors.toCollection(sortedSetSupplier),
        s -> {
          switch (s.size()) {
            case 0:
              return Collections.emptySortedSet();
            default:
              return Collections.unmodifiableSortedSet(s);
          }
        });
  }

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link NavigableSet}. If the
   * result is empty, then your {@link Set} will not be used.
   */
  public static <T, S extends NavigableSet<T>> Collector<T, ?, NavigableSet<T>> toUnmodifiableNavigableSet(
      @Nonnull final Supplier<S> navigableSetSupplier) {
    return Collectors.collectingAndThen(Collectors.toCollection(navigableSetSupplier),
        s -> {
          switch (s.size()) {
            case 0:
              return Collections.emptyNavigableSet();
            default:
              return Collections.unmodifiableNavigableSet(s);
          }
        });
  }

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link Map}.
   *
   * @see Collectors#toMap(Function, Function)
   */
  public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableMap(
      @Nonnull final Function<? super T, ? extends K> keyMapper,
      @Nonnull final Function<? super T, ? extends U> valueMapper) {
    return Collectors.collectingAndThen(Collectors.toMap(keyMapper, valueMapper),
        RSCollectors::unmodifiableMapFinisher);
  }

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link Map}, where you can
   * specify the underlying {@link Map}. If the result is empty or has one entry, then your
   * {@link Map} will not be used.
   *
   * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
   * @see #throwingMerger()
   */
  public static <T, K, U, M extends Map<K, U>> Collector<T, ?, Map<K, U>> toUnmodifiableMap(
      @Nonnull final Function<? super T, ? extends K> keyMapper,
      @Nonnull final Function<? super T, ? extends U> valueMapper,
      @Nonnull final BinaryOperator<U> mergeFunction, @Nonnull final Supplier<M> mapSupplier) {
    return Collectors.collectingAndThen(
        Collectors.toMap(keyMapper, valueMapper, mergeFunction, mapSupplier),
        RSCollectors::unmodifiableMapFinisher);
  }

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link Map} that's
   * <em>likely</em> backed by an {@link EnumMap}.
   */
  public static <T, K extends Enum<K>, U> Collector<T, ?, Map<K, U>> toUnmodifiableEnumMap(
      @Nonnull final Function<? super T, ? extends K> keyMapper,
      @Nonnull final Function<? super T, ? extends U> valueMapper, @Nonnull final Class<K> clazz) {
    return toUnmodifiableMap(keyMapper, valueMapper, throwingMerger(), () -> new EnumMap<>(clazz));
  }

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link SortedMap}. If the result
   * is empty, then your {@link Map} will not be used.
   *
   * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
   * @see #throwingMerger()
   */
  public static <T, K, U, M extends SortedMap<K, U>> Collector<T, ?, SortedMap<K, U>> toUnmodifiableSortedMap(
      @Nonnull final Function<? super T, ? extends K> keyMapper,
      @Nonnull final Function<? super T, ? extends U> valueMapper,
      @Nonnull final BinaryOperator<U> mergeFunction, @Nonnull final Supplier<M> mapSupplier) {
    return Collectors.collectingAndThen(
        Collectors.toMap(keyMapper, valueMapper, mergeFunction, mapSupplier),
        m -> {
          switch (m.size()) {
            case 0:
              return Collections.emptySortedMap();
            default:
              return Collections.unmodifiableSortedMap(m);
          }
        });
  }

  /**
   * {@link Collector} that collects elements into an unmodifiable {@link NavigableMap}. If the
   * result is empty, then your {@link Map} will not be used.
   *
   * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
   * @see #throwingMerger()
   */
  public static <T, K, U, M extends NavigableMap<K, U>> Collector<T, ?, NavigableMap<K, U>> toUnmodifiableNavigableMap(
      @Nonnull final Function<? super T, ? extends K> keyMapper,
      @Nonnull final Function<? super T, ? extends U> valueMapper,
      @Nonnull final BinaryOperator<U> mergeFunction, @Nonnull final Supplier<M> mapSupplier) {
    return Collectors.collectingAndThen(
        Collectors.toMap(keyMapper, valueMapper, mergeFunction, mapSupplier),
        m -> {
          switch (m.size()) {
            case 0:
              return Collections.emptyNavigableMap();
            default:
              return Collections.unmodifiableNavigableMap(m);
          }
        });
  }

  /**
   * Stolen from {@link Collectors} and made public. Used as the mergerFunction in {@link Map}
   * collectors.
   */
  public static <T> BinaryOperator<T> throwingMerger() {
    return (u, v) -> {
      throw new IllegalStateException("Duplicate key " + u);
    };
  }

  private static <T> Set<T> unmodifiableSetFinisher(@Nonnull final Set<T> s) {
    switch (s.size()) {
      case 0:
        return Collections.emptySet();
      case 1:
        return Collections.singleton(s.iterator().next());
      default:
        return Collections.unmodifiableSet(s);
    }
  }

  private static <K, U> Map<K, U> unmodifiableMapFinisher(
      @Nonnull final Map<? extends K, ? extends U> m) {
    switch (m.size()) {
      case 0:
        return Collections.emptyMap();
      case 1:
        final Map.Entry<? extends K, ? extends U> firstEntry = m.entrySet().iterator().next();
        return Collections.singletonMap(firstEntry.getKey(), firstEntry.getValue());
      default:
        return Collections.unmodifiableMap(m);
    }
  }

}

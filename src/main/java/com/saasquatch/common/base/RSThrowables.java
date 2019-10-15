package com.saasquatch.common.base;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import com.saasquatch.common.collect.RSCollectors;

/**
 * Utilities for {@link Throwable}s.
 *
 * @author sli
 */
public final class RSThrowables {

  static final int DEFAULT_CAUSE_CHAIN_LIMIT = 100;

  private RSThrowables() {}

  /**
   * Get the cause chain with limit 100.
   */
  public static List<Throwable> getCauseChainList(@Nonnull Throwable t) {
    return getCauseChainList(t, DEFAULT_CAUSE_CHAIN_LIMIT);
  }

  /**
   * Get the cause chain as a {@link List}
   *
   * @param limit the limit for the cause chain. 0 for unlimited.
   */
  public static List<Throwable> getCauseChainList(@Nonnull Throwable t, int limit) {
    return getCauseChainStream(t, limit).collect(RSCollectors.toUnmodifiableList());
  }

  /**
   * Get the cause chain with limit 100.
   */
  public static Stream<Throwable> getCauseChainStream(@Nonnull Throwable t) {
    return getCauseChainStream(t, DEFAULT_CAUSE_CHAIN_LIMIT);
  }

  /**
   * Get the cause chain as a {@link Stream}
   *
   * @param limit the limit for the cause chain. 0 for unlimited.
   */
  public static Stream<Throwable> getCauseChainStream(@Nonnull Throwable t, int limit) {
    return StreamSupport.stream(getCauseChain(t, limit).spliterator(), false);
  }

  /**
   * Get the cause chain with limit 100.
   */
  public static Iterable<Throwable> getCauseChain(@Nonnull Throwable t) {
    return getCauseChain(t, DEFAULT_CAUSE_CHAIN_LIMIT);
  }

  /**
   * Get the cause chain with an arbitrary limit.
   *
   * @param limit The limit for the cause chain. 0 for unlimited.
   */
  public static Iterable<Throwable> getCauseChain(@Nonnull Throwable t, int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("Negative limit");
    }
    return new CauseChainIterable(Objects.requireNonNull(t), limit);
  }

  /**
   * Find the first Throwable in the cause chain that is an instance of the given exception class.
   *
   * @see #getCauseChain(Throwable)
   */
  public static <X extends Throwable> Optional<? extends X> findFirstInCauseChain(
      @Nonnull Throwable t, @Nonnull Class<? extends X> exceptionClass) {
    return getCauseChainStream(t)
        .filter(exceptionClass::isInstance)
        .map(exceptionClass::cast)
        .findFirst();
  }

  /**
   * Unwrap the given Throwable, and throw the unwrapped Exception if it's the given type.<br>
   * Example usage:
   *
   * <pre>
   * void foo() throws IOException, ParseException {
   *   try {
   *     // ...
   *   } catch (RuntimeException e) {
   *     unwrapAndThrow(e, IOException.class);
   *     unwrapAndThrow(e, ParseException.class);
   *     throw e;
   *   }
   * }
   * </pre>
   */
  public static <X extends Throwable> void unwrapAndThrow(@Nonnull Throwable t,
      @Nonnull Class<? extends X> exceptionClass) throws X {
    final X ex = findFirstInCauseChain(t, exceptionClass).orElse(null);
    if (ex != null) {
      throw ex;
    }
  }

  /**
   * Inspired by lang3 and jOOL.<br>
   * Throw the given {@link Throwable} if it's a {@link RuntimeException} or an {@link Error}, or
   * wrap it with a {@link UndeclaredThrowableException} and throw it.
   */
  public static <R> R wrapAndThrow(@Nonnull final Throwable t) {
    Objects.requireNonNull(t);
    if (t instanceof RuntimeException) {
      throw (RuntimeException) t;
    }
    if (t instanceof Error) {
      throw (Error) t;
    }
    if (t instanceof IOException) {
      throw new UncheckedIOException((IOException) t);
    }
    if (t instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    throw new UndeclaredThrowableException(t);
  }

  private static class CauseChainIterable implements Iterable<Throwable> {

    private final Throwable t;
    private final int limit;

    CauseChainIterable(Throwable t, int limit) {
      this.t = t;
      this.limit = limit;
    }

    @Override
    public Iterator<Throwable> iterator() {
      return new CauseChainIterator(t, limit);
    }

    @Override
    public Spliterator<Throwable> spliterator() {
      return Spliterators.spliteratorUnknownSize(iterator(),
          Spliterator.NONNULL | Spliterator.ORDERED);
    }

  }

  private static class CauseChainIterator implements Iterator<Throwable> {

    private int count = 0;
    private Throwable next;
    private Throwable curr;
    private final int limit;

    CauseChainIterator(Throwable t, int limit) {
      this.next = t;
      this.limit = limit;
    }

    @Override
    public boolean hasNext() {
      if (limit > 0 && count >= limit) {
        return false;
      }
      if (count == 0) {
        // The root is always available
        return true;
      }
      return next != null && next != curr && !next.equals(curr);
    }

    @Override
    public Throwable next() {
      count++;
      curr = next;
      next = next.getCause();
      return curr;
    }

  }

}

package saasquatch.common.base;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Utils for {@link Throwable}s.
 *
 * @author sli
 */
public class RSThrowables {

  /**
   * Convenience method for {@link #getCauseChain(Throwable)}
   */
  public static List<Throwable> getCauseChainAsList(@Nonnull Throwable t) {
    final ArrayList<Throwable> result = new ArrayList<>();
    getCauseChain(t).forEach(result::add);
    result.trimToSize();
    return Collections.unmodifiableList(result);
  }

  /**
   * Get the cause chain with limit 100.
   *
   * @param t
   * @return
   */
  public static Iterable<Throwable> getCauseChain(@Nonnull Throwable t) {
    return getCauseChain(t, 100);
  }

  /**
   * Get the cause chain with an arbitrary limit.
   *
   * @param t
   * @param limit The limit for the cause chain. -1 for unlimited.
   */
  public static Iterable<Throwable> getCauseChain(@Nonnull Throwable t, int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("Negative limit");
    }
    return () -> new CauseIterator(t, limit);
  }

  /**
   * Find the first Throwable in the cause chain that is an instance of the given exception class.
   * @see #getCauseChain(Throwable)
   */
  public static <X extends Exception> X findFirstInCauseChain(@Nonnull Throwable t,
      Class<? extends X> exceptionClass) {
    for (final Throwable curr : getCauseChain(t)) {
      if (exceptionClass.isInstance(curr)) {
        return exceptionClass.cast(curr);
      }
    }
    return null;
  }

  /**
   * Unwrap the given Throwable, and throw the unwrapped Exception if it's the given type.<br>
   * Example usage:
   *
   * <pre>
   * void foo() throws IOException {
   *   try {
   *     // ...
   *   } catch (RuntimeException e) {
   *     unwrapAndThrow(e, IOException.class);
   *     throw e;
   *   }
   * }
   * </pre>
   */
  public static <X extends Exception> void unwrapAndThrow(@Nonnull Throwable t,
      Class<? extends X> exceptionClass) throws X {
    final X ex = findFirstInCauseChain(t, exceptionClass);
    if (ex != null)
      throw ex;
  }

  /**
   * Inspired by lang3 and jOOL.
   */
  public static <R> R wrapAndThrow(final Throwable t) {
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
    throw new RuntimeException(t);
  }

  private static class CauseIterator implements Iterator<Throwable> {

    private int currIdx = 0;
    private Throwable next;
    private Throwable curr;
    private final int limit;

    public CauseIterator(Throwable t, int limit) {
      this.next = t;
      this.limit = limit == 0 ? Integer.MAX_VALUE : limit;
    }

    @Override
    public boolean hasNext() {
      if (currIdx >= limit) {
        return false;
      }
      return next != null && !next.equals(curr);
    }

    @Override
    public Throwable next() {
      currIdx++;
      curr = next;
      next = next.getCause();
      return curr;
    }

  }

}

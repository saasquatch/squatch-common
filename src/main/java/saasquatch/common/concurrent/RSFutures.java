package saasquatch.common.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.annotation.Nonnull;
import saasquatch.common.collect.RSCollectors;

/**
 * Utilities for futures
 *
 * @author sli
 */
public class RSFutures {

  /**
   * Convert a {@link Collection} of {@link CompletionStage}s into a
   * {@code CompletionStage<List<A>>}
   */
  public static <A> CompletionStage<List<A>> sequence(
      @Nonnull final Collection<? extends CompletionStage<? extends A>> promises) {
    return CompletableFuture.allOf(toCompletableFutureArray(promises))
        .thenApply(sequenceHandler(promises));
  }

  /**
   * Async version of {@link #sequence(Collection)}
   */
  public static <A> CompletionStage<List<A>> sequenceAsync(
      @Nonnull final Collection<? extends CompletionStage<? extends A>> promises) {
    return CompletableFuture.allOf(toCompletableFutureArray(promises))
        .thenApplyAsync(sequenceHandler(promises));
  }

  /**
   * Async version of {@link #sequence(Collection)} with a custom {@link Executor}
   */
  public static <A> CompletionStage<List<A>> sequenceAsync(
      @Nonnull final Collection<? extends CompletionStage<? extends A>> promises,
      @Nonnull final Executor executor) {
    return CompletableFuture.allOf(toCompletableFutureArray(promises))
        .thenApplyAsync(sequenceHandler(promises), executor);
  }

  private static CompletableFuture<?>[] toCompletableFutureArray(
      @Nonnull final Collection<? extends CompletionStage<?>> promises) {
    return promises.stream().map(CompletionStage::toCompletableFuture)
        .toArray(CompletableFuture[]::new);
  }

  private static <A> Function<Void, List<A>> sequenceHandler(
      @Nonnull final Collection<? extends CompletionStage<? extends A>> promises) {
    return _ignored -> promises.stream()
        .map(CompletionStage::toCompletableFuture)
        /*
         * By the time join gets called, all the promises are guaranteed to have completed, so this
         * will not block any thread.
         */
        .map(CompletableFuture::join)
        .collect(RSCollectors.toUnmodifiableList());
  }

}

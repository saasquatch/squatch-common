package com.saasquatch.common.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import com.saasquatch.common.collect.RSCollectors;

/**
 * Utilities for futures
 *
 * @author sli
 */
public final class RSFutures {

  private RSFutures() {}

  /**
   * Combine the results of {@link Collection} of {@link CompletionStage}s into a
   * {@code CompletionStage<List<V>>}. The resulting {@link List} will be unmodifiable.
   */
  public static <V> CompletionStage<List<V>> sequence(
      @Nonnull final Collection<? extends CompletionStage<? extends V>> promises) {
    Objects.requireNonNull(promises);
    final CompletableFuture<? extends V>[] cfArray = toCfArray(promises);
    return CompletableFuture.allOf(cfArray)
        .thenApply(ignored -> sequenceHandlerAfterAllOf(cfArray));
  }

  /**
   * Async version of {@link #sequence(Collection)}
   */
  public static <V> CompletionStage<List<V>> sequenceAsync(
      @Nonnull final Collection<? extends CompletionStage<? extends V>> promises) {
    Objects.requireNonNull(promises);
    final CompletableFuture<? extends V>[] cfArray = toCfArray(promises);
    return CompletableFuture.allOf(cfArray)
        .thenApplyAsync(ignored -> sequenceHandlerAfterAllOf(cfArray));
  }

  /**
   * Async version of {@link #sequence(Collection)} with a custom {@link Executor}
   */
  public static <V> CompletionStage<List<V>> sequenceAsync(
      @Nonnull final Collection<? extends CompletionStage<? extends V>> promises,
      @Nonnull final Executor executor) {
    Objects.requireNonNull(promises);
    Objects.requireNonNull(executor);
    final CompletableFuture<? extends V>[] cfArray = toCfArray(promises);
    return CompletableFuture.allOf(cfArray)
        .thenApplyAsync(ignored -> sequenceHandlerAfterAllOf(cfArray), executor);
  }

  private static <V> CompletableFuture<V>[] toCfArray(
      @Nonnull final Collection<? extends CompletionStage<V>> promises) {
    @SuppressWarnings("unchecked")
    final CompletableFuture<V>[] cfArr = promises.stream()
        .<CompletableFuture<V>>map(CompletionStage::toCompletableFuture)
        .toArray(CompletableFuture[]::new);
    return cfArr;
  }

  private static <V> List<V> sequenceHandlerAfterAllOf(
      @Nonnull CompletableFuture<? extends V>[] promises) {
    return Arrays.stream(promises)
        /*
         * By the time join gets called, all the promises are guaranteed to have completed, so this
         * will not block a thread.
         */
        .map(CompletableFuture::join)
        .collect(RSCollectors.toUnmodifiableList());
  }

  /**
   * Similar to {@link CompletableFuture#supplyAsync(Supplier)}, but takes a {@link Supplier} of
   * {@link CompletionStage}.
   */
  public static <V> CompletionStage<V> submitAsync(
      @Nonnull final Supplier<? extends CompletionStage<V>> promiseSupplier) {
    Objects.requireNonNull(promiseSupplier);
    return CompletableFuture.completedFuture(null)
        .thenComposeAsync(ignored -> promiseSupplier.get());
  }

  /**
   * Similar to {@link CompletableFuture#supplyAsync(Supplier, Executor)}, but takes a
   * {@link Supplier} of {@link CompletionStage}.
   */
  public static <V> CompletionStage<V> submitAsync(
      @Nonnull final Supplier<? extends CompletionStage<V>> promiseSupplier,
      @Nonnull final Executor executor) {
    Objects.requireNonNull(promiseSupplier);
    Objects.requireNonNull(executor);
    return CompletableFuture.completedFuture(null)
        .thenComposeAsync(ignored -> promiseSupplier.get(), executor);
  }

  /**
   * Creates a {@link CompletionStage} that immediately fails with the given {@link Throwable}.
   */
  public static <V> CompletionStage<V> failedFuture(@Nonnull final Throwable t) {
    Objects.requireNonNull(t);
    final CompletableFuture<V> cf = new CompletableFuture<>();
    cf.completeExceptionally(t);
    return cf;
  }

}

package com.saasquatch.common.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RSFuturesTest {

  private static final Executor tptExecutor = RSExecutors.threadPerTaskExecutor(true);
  private static ScheduledExecutorService scheduledExecutor;

  @BeforeAll
  public static void beforeAll() {
    scheduledExecutor = Executors.newScheduledThreadPool(1, RSExecutors.simpleThreadFactory(true));
  }

  @AfterAll
  public static void afterAll() {
    scheduledExecutor.shutdown();
  }

  @Test
  public void testSequence() throws Exception {
    assertThrows(NullPointerException.class, () -> RSFutures.sequence(null));
    assertThrows(NullPointerException.class,
        () -> RSFutures.sequence(Collections.singletonList(null)));
    doTestSequence(RSFutures::sequence);
  }

  @Test
  public void testSequenceAsync() throws Exception {
    assertThrows(NullPointerException.class, () -> RSFutures.sequenceAsync(null));
    assertThrows(NullPointerException.class,
        () -> RSFutures.sequenceAsync(Collections.singletonList(null)));
    doTestSequence(RSFutures::sequenceAsync);
  }

  @Test
  public void testSequenceAsyncExecutor() throws Exception {
    assertThrows(NullPointerException.class,
        () -> RSFutures.sequenceAsync(Collections.emptyList(), null));
    assertThrows(NullPointerException.class, () -> RSFutures.sequenceAsync(null, tptExecutor));
    assertThrows(NullPointerException.class,
        () -> RSFutures.sequenceAsync(Collections.singletonList(null), tptExecutor));
    final ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      doTestSequence(promises -> RSFutures.sequenceAsync(promises, executor));
    } finally {
      executor.shutdown();
    }
  }

  private void doTestSequence(
      Function<List<CompletionStage<Integer>>, CompletionStage<List<Integer>>> sequenceFunc)
      throws Exception {
    final List<Integer> intList = ThreadLocalRandom.current().ints(1024, 0, 256)
        .boxed()
        .collect(Collectors.toList());
    final List<CompletionStage<Integer>> promiseList = intList.stream()
        .map(i -> {
          final CompletableFuture<Integer> delayed = new CompletableFuture<>();
          scheduledExecutor.schedule(() -> delayed.complete(i), i, TimeUnit.MILLISECONDS);
          return delayed;
        })
        .collect(Collectors.toList());
    final List<Integer> sequencedIntList = sequenceFunc.apply(promiseList)
        .toCompletableFuture()
        .get(10, TimeUnit.SECONDS);
    assertEquals(intList, sequencedIntList, "We should get the same list");
    assertThrows(UnsupportedOperationException.class, () -> sequencedIntList.add(0));
  }

  @Test
  public void testSubmitAsync() throws Exception {
    assertThrows(NullPointerException.class, () -> RSFutures.submitAsync(null));
    doTestSubmit(RSFutures::submitAsync);
  }

  @Test
  public void testSubmitAsyncExecutor() throws Exception {
    assertThrows(NullPointerException.class, () -> RSFutures.submitAsync(null, tptExecutor));
    assertThrows(NullPointerException.class, () -> RSFutures.submitAsync(() -> null, null));
    final ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      doTestSubmit(promise -> RSFutures.submitAsync(promise, executor));
    } finally {
      executor.shutdown();
    }
  }

  private void doTestSubmit(
      Function<Supplier<CompletionStage<Integer>>, CompletionStage<Integer>> submitFunc)
      throws Exception {
    final int val = ThreadLocalRandom.current().nextInt();
    final long callingThreadId = Thread.currentThread().getId();
    final CompletableFuture<Integer> promise = submitFunc.apply(() -> {
      assertNotEquals(callingThreadId, Thread.currentThread().getId());
      final CompletableFuture<Integer> delayed = new CompletableFuture<>();
      scheduledExecutor.schedule(() -> delayed.complete(val), 100, TimeUnit.MILLISECONDS);
      return delayed;
    }).toCompletableFuture();
    assertThrows(TimeoutException.class, () -> promise.get(10, TimeUnit.MILLISECONDS));
    assertEquals(val, promise.get(100, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testFailedFuture() {
    assertThrows(NullPointerException.class, () -> RSFutures.failedFuture(null));
    final IOException ioe = new IOException("foo");
    final CompletableFuture<String> failedFuture =
        RSFutures.<String>failedFuture(ioe).toCompletableFuture();
    assertTrue(failedFuture.isCompletedExceptionally());
    try {
      failedFuture.join();
      fail("We should get an Exception");
    } catch (RuntimeException e) {
      assertSame(ioe, e.getCause());
    }
  }

}

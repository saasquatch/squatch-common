package com.saasquatch.common.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RSFuturesTest {

  private static ScheduledExecutorService scheduledExecutor;

  @BeforeAll
  public static void beforeAll() {
    scheduledExecutor = Executors.newScheduledThreadPool(1);
  }

  @AfterAll
  public static void afterAll() {
    scheduledExecutor.shutdown();
  }

  @Test
  public void testSequence() throws Exception {
    doTestSequence(RSFutures::sequence);
  }

  @Test
  public void testSequenceAsync() throws Exception {
    doTestSequence(RSFutures::sequenceAsync);
  }

  @Test
  public void testSequenceAsyncExecutor() throws Exception {
    final ExecutorService executor = Executors.newFixedThreadPool(8);
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
  }

}

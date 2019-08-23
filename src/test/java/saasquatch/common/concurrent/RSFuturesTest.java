package saasquatch.common.concurrent;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;

public class RSFuturesTest {

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
    doTestSequence(promises -> RSFutures.sequenceAsync(promises, ForkJoinPool.commonPool()));
  }

  private static void doTestSequence(
      Function<List<CompletionStage<Integer>>, CompletionStage<List<Integer>>> sequenceFunc)
      throws Exception {
    final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    try {
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
      assertEquals("We should get the same list", intList, sequencedIntList);
    } finally {
      scheduledExecutor.shutdown();
    }
  }

}

package saasquatch.common.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

public class RSExecutorsTest {

  @Test
  public void testThreadPerTaskExecutorWorks() {
    final Executor exec1 = RSExecutors.threadPerTaskExecutor(true);
    final Executor exec2 = RSExecutors.threadPerTaskExecutor(false);
    final CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(() -> 1, exec1);
    final CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> 2, exec2);
    assertEquals(Arrays.asList(1, 2),
        RSFutures.sequence(Arrays.asList(f1, f2)).toCompletableFuture().join());
  }

  @Test
  public void testThreadGroup() {
    CompletableFuture.runAsync(() -> {
      assertEquals("RSExecutors.threadPerTaskExecutor(daemon)",
          Thread.currentThread().getThreadGroup().getName());
    }, RSExecutors.threadPerTaskExecutor(true)).join();
    CompletableFuture.runAsync(() -> {
      assertEquals("RSExecutors.threadPerTaskExecutor(non-daemon)",
          Thread.currentThread().getThreadGroup().getName());
    }, RSExecutors.threadPerTaskExecutor(false)).join();
  }

}

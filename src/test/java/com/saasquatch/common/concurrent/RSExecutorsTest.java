package com.saasquatch.common.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;
import com.google.common.util.concurrent.Runnables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RSExecutorsTest {

  private static final String threadFactoryName = RSSimpleThreadFactory.threadGroup.getName();

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
  public void testThreadPerTaskExecutorThreadName() {
    CompletableFuture.runAsync(() -> {
      assertEquals(threadFactoryName, Thread.currentThread().getThreadGroup().getName());
      assertTrue(Thread.currentThread().getName()
          .startsWith("RSExecutors.threadPerTaskExecutor(daemon)-"));
    }, RSExecutors.threadPerTaskExecutor(true)).join();
    CompletableFuture.runAsync(() -> {
      assertEquals(threadFactoryName, Thread.currentThread().getThreadGroup().getName());
      assertTrue(Thread.currentThread().getName()
          .startsWith("RSExecutors.threadPerTaskExecutor(non-daemon)-"));
    }, RSExecutors.threadPerTaskExecutor(false)).join();
  }

  @Test
  public void testSimpleThreadFactoryWrapping() {
    {
      final Thread newThread =
          RSExecutors.simpleThreadFactory(true).newThread(Runnables.doNothing());
      assertEquals("", newThread.getName());
    }
    {
      final ThreadFactory threadFactory = new ThreadFactoryBuilder()
          .setThreadFactory(RSExecutors.simpleThreadFactory(true)).setNameFormat("foo-%d").build();
      final Thread newThread0 = threadFactory.newThread(Runnables.doNothing());
      final Thread newThread1 = threadFactory.newThread(Runnables.doNothing());
      assertEquals("foo-0", newThread0.getName());
      assertEquals("foo-1", newThread1.getName());
    }
  }

  @Test
  public void testSimpleThreadFactoryDaemon() {
    assertTrue(RSExecutors.simpleThreadFactory(true).newThread(Runnables.doNothing()).isDaemon());
    assertFalse(RSExecutors.simpleThreadFactory(false).newThread(Runnables.doNothing()).isDaemon());
    assertTrue(new ThreadFactoryBuilder().setThreadFactory(RSExecutors.simpleThreadFactory(false))
        .setDaemon(true).build().newThread(Runnables.doNothing()).isDaemon());
  }

}

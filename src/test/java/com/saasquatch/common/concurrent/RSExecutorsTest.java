package com.saasquatch.common.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RSExecutorsTest {

  private static final Runnable EMPTY_RUNNABLE = () -> {
  };

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
      assertEquals(RSSimpleThreadFactory.DAEMON.baseName,
          Thread.currentThread().getThreadGroup().getName());
      assertTrue(Thread.currentThread().getName()
          .startsWith("RSExecutors.threadPerTaskExecutor(daemon)-"));
    }, RSExecutors.threadPerTaskExecutor(true)).join();
    CompletableFuture.runAsync(() -> {
      assertEquals(RSSimpleThreadFactory.NON_DAEMON.baseName,
          Thread.currentThread().getThreadGroup().getName());
      assertTrue(Thread.currentThread().getName()
          .startsWith("RSExecutors.threadPerTaskExecutor(non-daemon)-"));
    }, RSExecutors.threadPerTaskExecutor(false)).join();
  }

  @Test
  public void testSimpleThreadFactoryWrapping() {
    {
      final Thread newThread =
          RSExecutors.simpleThreadFactory(true).newThread(EMPTY_RUNNABLE);
      assertEquals("", newThread.getName());
    }
    {
      final ThreadFactory threadFactory = new ThreadFactoryBuilder()
          .setThreadFactory(RSExecutors.simpleThreadFactory(true)).setNameFormat("foo-%d").build();
      final Thread newThread0 = threadFactory.newThread(EMPTY_RUNNABLE);
      final Thread newThread1 = threadFactory.newThread(EMPTY_RUNNABLE);
      assertEquals("foo-0", newThread0.getName());
      assertEquals("foo-1", newThread1.getName());
    }
  }

  @Test
  public void testSimpleThreadFactoryDaemon() {
    assertTrue(RSExecutors.simpleThreadFactory(true).newThread(EMPTY_RUNNABLE).isDaemon());
    assertFalse(RSExecutors.simpleThreadFactory(false).newThread(EMPTY_RUNNABLE).isDaemon());
    assertTrue(new ThreadFactoryBuilder().setThreadFactory(RSExecutors.simpleThreadFactory(false))
        .setDaemon(true).build().newThread(EMPTY_RUNNABLE).isDaemon());
  }

  @Test
  public void testSimpleThreadFactoryPriority() {
    CompletableFuture.runAsync(() -> {
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      CompletableFuture.runAsync(() -> {
        assertEquals(Thread.NORM_PRIORITY, Thread.currentThread().getPriority());
      }, RSExecutors.threadPerTaskExecutor(true)).join();
    }, RSExecutors.threadPerTaskExecutor(true)).join();
  }

}

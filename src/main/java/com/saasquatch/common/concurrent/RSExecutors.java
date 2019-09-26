package com.saasquatch.common.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Utilities for {@link Executor}s.
 *
 * @author sli
 */
public final class RSExecutors {

  private RSExecutors() {}

  /**
   * A <em>simple</em> {@link ThreadFactory} where the {@link Thread}s are always created with a
   * singleton {@link ThreadFactory} used only for tracking, always with
   * {@link Thread#NORM_PRIORITY}, and with the name for the threads always being {@code ""}. The
   * returning {@link ThreadFactory} is meant to be wrapped in another {@link ThreadFactory} like
   * Guava's {@code ThreadFactoryBuilder} where you can specify whether you want daemon threads and
   * your own thread name format.
   *
   * @return a singleton {@link ThreadFactory}
   */
  public static ThreadFactory simpleThreadFactory(boolean daemon) {
    return daemon ? RSSimpleThreadFactory.DAEMON : RSSimpleThreadFactory.NON_DAEMON;
  }

  /**
   * {@link Executor} where a new {@link Thread} is created per task.
   *
   * @return a singleton {@link Executor}
   */
  public static Executor threadPerTaskExecutor(boolean daemon) {
    return daemon ? RSThreadPerTaskExecutor.DAEMON : RSThreadPerTaskExecutor.NON_DAEMON;
  }

}

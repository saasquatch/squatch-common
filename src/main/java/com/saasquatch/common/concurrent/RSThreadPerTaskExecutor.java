package com.saasquatch.common.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Self-explanatory
 *
 * @author sli
 * @see RSExecutors#threadPerTaskExecutor(boolean)
 */
enum RSThreadPerTaskExecutor implements Executor {

  DAEMON(true), NON_DAEMON(false),;

  private final AtomicLong threadIndex = new AtomicLong();
  private final String baseThreadName;
  private final boolean daemon;

  RSThreadPerTaskExecutor(boolean daemon) {
    this.daemon = daemon;
    this.baseThreadName = String.format("%s.threadPerTaskExecutor(%sdaemon)-",
        RSExecutors.class.getSimpleName(), daemon ? "" : "non-");
  }

  @Override
  public void execute(Runnable command) {
    final Thread t = RSExecutors.simpleThreadFactory(daemon).newThread(command);
    t.setName(baseThreadName + threadIndex.getAndIncrement());
    t.start();
  }

}

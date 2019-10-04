package com.saasquatch.common.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import com.saasquatch.common.base.RSStrings;

/**
 * Self-explanatory
 *
 * @author sli
 * @see RSExecutors#threadPerTaskExecutor(boolean)
 */
enum RSThreadPerTaskExecutor implements Executor {

  DAEMON(true), NON_DAEMON(false),;

  private final AtomicLong threadIndex = new AtomicLong();
  private final String baseName;
  private final boolean daemon;

  RSThreadPerTaskExecutor(boolean daemon) {
    this.daemon = daemon;
    this.baseName = RSStrings.format("%s.threadPerTaskExecutor(%sdaemon)",
        RSExecutors.class.getSimpleName(), daemon ? "" : "non-");
  }

  @Override
  public void execute(Runnable command) {
    final Thread t = RSExecutors.simpleThreadFactory(daemon).newThread(command);
    t.setName(baseName + '-' + threadIndex.getAndIncrement());
    t.start();
  }

  @Override
  public String toString() {
    return baseName;
  }

}

package com.saasquatch.common.concurrent;

import java.util.concurrent.ThreadFactory;
import com.saasquatch.common.base.RSStrings;

/**
 * A singleton <em>simple</em> {@link ThreadFactory}
 *
 * @author sli
 * @see RSExecutors#simpleThreadFactory()
 */
enum RSSimpleThreadFactory implements ThreadFactory {

  DAEMON(true), NON_DAEMON(false),;

  private final boolean daemon;
  // Visible for testing
  final String baseName;
  private final ThreadGroup threadGroup;

  RSSimpleThreadFactory(boolean daemon) {
    this.daemon = daemon;
    this.baseName = RSStrings.format("%s.simpleThreadFactory(%sdaemon)",
        RSExecutors.class.getSimpleName(), daemon ? "" : "non-");
    this.threadGroup = new ThreadGroup(baseName);
  }

  @Override
  public Thread newThread(Runnable r) {
    final Thread t = new Thread(threadGroup, r, "");
    if (t.isDaemon() != daemon) {
      t.setDaemon(daemon);
    }
    if (t.getPriority() != Thread.NORM_PRIORITY) {
      t.setPriority(Thread.NORM_PRIORITY);
    }
    return t;
  }

  @Override
  public String toString() {
    return baseName;
  }

}

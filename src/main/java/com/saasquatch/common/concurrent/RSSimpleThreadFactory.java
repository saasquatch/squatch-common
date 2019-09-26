package com.saasquatch.common.concurrent;

import java.util.concurrent.ThreadFactory;

/**
 * A singleton <em>simple</em> {@link ThreadFactory}
 *
 * @author sli
 * @see RSExecutors#simpleThreadFactory()
 */
enum RSSimpleThreadFactory implements ThreadFactory {

  DAEMON(true), NON_DAEMON(false),;

  // Visible for testing
  static final ThreadGroup threadGroup =
      new ThreadGroup(String.format("%s.simpleThreadFactory", RSExecutors.class.getSimpleName()));

  private final boolean daemon;

  RSSimpleThreadFactory(boolean daemon) {
    this.daemon = daemon;
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

}

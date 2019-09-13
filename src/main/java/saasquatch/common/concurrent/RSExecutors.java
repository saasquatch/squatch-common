package saasquatch.common.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;

/**
 * Utilities for {@link Executor}s.
 *
 * @author sli
 */
public final class RSExecutors {

  private RSExecutors() {}

  /**
   * {@link Executor} where a new {@link Thread} is created per task.
   *
   * @return a singleton {@link Executor}
   */
  public static Executor threadPerTaskExecutor(boolean daemon) {
    return daemon ? TPTExecutor.DAEMON : TPTExecutor.NON_DAEMON;
  }

  private static enum TPTExecutor implements Executor {
    DAEMON(true), NON_DAEMON(false),;

    private final boolean daemon;
    private final ThreadGroup threadGroup;

    private TPTExecutor(boolean daemon) {
      this.daemon = daemon;
      this.threadGroup = new ThreadGroup(String.format("%s.threadPerTaskExecutor(%sdaemon)",
          RSExecutors.class.getSimpleName(), daemon ? "" : "non-"));
    }

    @Override
    public void execute(Runnable command) {
      final Thread t = new Thread(threadGroup, command);
      t.setDaemon(daemon);
      t.start();
    }
  }

  public static ExecutorService asExecutorService(@Nonnull Executor executor) {
    if (executor instanceof ExecutorService) {
      return (ExecutorService) executor;
    }
    return new ExecutorExecutorService(executor);
  }

}

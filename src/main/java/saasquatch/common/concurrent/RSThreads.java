package saasquatch.common.concurrent;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import saasquatch.common.base.RSThrowables;

public class RSThreads {
  private static final Logger logger = LoggerFactory.getLogger(RSThreads.class);

  private static Field threadLocalsField;

  /**
   * The threadLocals field on a {@link Thread} object. We want to nuke this field after execution
   * to prevent memory leaks caused by {@link ThreadLocal}s.
   */
  public static void clearThreadLocals(Thread t) {
    try {
      Field tlField = threadLocalsField;
      if (tlField == null) {
        tlField = Thread.class.getDeclaredField("threadLocals");
        tlField.setAccessible(true);
        threadLocalsField = tlField;
      }
      tlField.set(t, null);
    } catch (Exception e) {
      logger.error("Error encountered when cleaning threadLocals", e);
      RSThrowables.wrapAndThrow(e);
    }
  }

  /**
   * Get a <em>full</em> thread dump using {@link ManagementFactory#getThreadMXBean()}.
   * @see #threadInfoToString(ThreadInfo)
   */
  public static String fullThreadDump() {
    return Arrays.stream(ManagementFactory.getThreadMXBean().dumpAllThreads(true, true))
        .map(RSThreads::threadInfoToString)
        .collect(Collectors.joining());
  }

  /**
   * Same as {@link ThreadInfo#toString()}, but removes the arbitrary stack trace limit.
   */
  public static String threadInfoToString(ThreadInfo threadInfo) {
    StringBuilder sb = new StringBuilder("\"" + threadInfo.getThreadName() + "\"" + " Id="
        + threadInfo.getThreadId() + " " + threadInfo.getThreadState());
    if (threadInfo.getLockName() != null) {
      sb.append(" on " + threadInfo.getLockName());
    }
    if (threadInfo.getLockOwnerName() != null) {
      sb.append(
          " owned by \"" + threadInfo.getLockOwnerName() + "\" Id=" + threadInfo.getLockOwnerId());
    }
    if (threadInfo.isSuspended()) {
      sb.append(" (suspended)");
    }
    if (threadInfo.isInNative()) {
      sb.append(" (in native)");
    }
    sb.append('\n');
    int i = 0;
    for (; i < threadInfo.getStackTrace().length /* && i < MAX_FRAMES */; i++) {
      StackTraceElement ste = threadInfo.getStackTrace()[i];
      sb.append("\tat " + ste.toString());
      sb.append('\n');
      if (i == 0 && threadInfo.getLockInfo() != null) {
        Thread.State ts = threadInfo.getThreadState();
        switch (ts) {
          case BLOCKED:
            sb.append("\t-  blocked on " + threadInfo.getLockInfo());
            sb.append('\n');
            break;
          case WAITING:
            sb.append("\t-  waiting on " + threadInfo.getLockInfo());
            sb.append('\n');
            break;
          case TIMED_WAITING:
            sb.append("\t-  waiting on " + threadInfo.getLockInfo());
            sb.append('\n');
            break;
          default:
        }
      }

      for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
        if (mi.getLockedStackDepth() == i) {
          sb.append("\t-  locked " + mi);
          sb.append('\n');
        }
      }
    }
    if (i < threadInfo.getStackTrace().length) {
      sb.append("\t...");
      sb.append('\n');
    }

    LockInfo[] locks = threadInfo.getLockedSynchronizers();
    if (locks.length > 0) {
      sb.append("\n\tNumber of locked synchronizers = " + locks.length);
      sb.append('\n');
      for (LockInfo li : locks) {
        sb.append("\t- " + li);
        sb.append('\n');
      }
    }
    sb.append('\n');
    return sb.toString();
  }

}

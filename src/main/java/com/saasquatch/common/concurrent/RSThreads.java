package com.saasquatch.common.concurrent;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import com.saasquatch.common.base.RSThrowables;

public final class RSThreads {

  private RSThreads() {}

  // Visible for testing
  static Field threadLocalsField;

  /**
   * The threadLocals field on a {@link Thread} object. We want to nuke this field after execution
   * to prevent memory leaks caused by {@link ThreadLocal}s.
   */
  public static void clearThreadLocals(@Nonnull Thread t) {
    Objects.requireNonNull(t);
    try {
      Field tlField = threadLocalsField;
      if (tlField == null) {
        tlField = Thread.class.getDeclaredField("threadLocals");
        tlField.setAccessible(true);
        threadLocalsField = tlField;
      }
      tlField.set(t, null);
    } catch (Exception e) {
      RSThrowables.wrapAndThrow(e);
    }
  }

  /**
   * Get a <em>full</em> thread dump using {@link ManagementFactory#getThreadMXBean()}.
   *
   * @see #threadInfoToString(ThreadInfo)
   */
  public static String fullThreadDump() {
    return Arrays.stream(ManagementFactory.getThreadMXBean().dumpAllThreads(true, true))
        .map(RSThreads::threadInfoToString)
        .collect(Collectors.joining());
  }

  /**
   * Similar to {@link ThreadInfo#toString()}, but removes the arbitrary stack trace limit.
   */
  public static String threadInfoToString(ThreadInfo threadInfo) {
    final StringBuilder sb = new StringBuilder();
    sb.append('"').append(threadInfo.getThreadName()).append('"').append(" Id=")
        .append(threadInfo.getThreadId()).append(' ').append(threadInfo.getThreadState());
    if (threadInfo.getLockName() != null) {
      sb.append(" on ").append(threadInfo.getLockName());
    }
    if (threadInfo.getLockOwnerName() != null) {
      sb.append(" owned by \"").append(threadInfo.getLockOwnerName()).append("\" Id=")
          .append(threadInfo.getLockOwnerId());
    }
    if (threadInfo.isInNative()) {
      sb.append(" (in native)");
    }
    sb.append('\n');

    if (threadInfo.getLockInfo() != null) {
      final Thread.State ts = threadInfo.getThreadState();
      switch (ts) {
        case BLOCKED:
          sb.append("\t-  blocked on ").append(threadInfo.getLockInfo()).append('\n');
          break;
        case WAITING:
          sb.append("\t-  waiting on ").append(threadInfo.getLockInfo()).append('\n');
          break;
        case TIMED_WAITING:
          sb.append("\t-  waiting on ").append(threadInfo.getLockInfo()).append('\n');
          break;
        default:
      }
    }

    for (int i = 0; i < threadInfo.getStackTrace().length; i++) {
      sb.append("\tat ").append(threadInfo.getStackTrace()[i]).append('\n');
      for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
        if (mi.getLockedStackDepth() == i) {
          sb.append("\t-  locked ").append(mi).append('\n');
        }
      }
    }

    final LockInfo[] locks = threadInfo.getLockedSynchronizers();
    if (locks.length > 0) {
      sb.append("\n\tNumber of locked synchronizers = ").append(locks.length).append('\n');
    }
    for (LockInfo li : locks) {
      sb.append("\t- ").append(li).append('\n');
    }
    sb.append('\n');
    return sb.toString();
  }

}

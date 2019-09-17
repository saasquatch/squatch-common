package saasquatch.common.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.security.Permission;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

public class RSThreadsTest {

  @Test
  public void testClearThreadLocals() {
    final Double val = Math.random();
    final ThreadLocal<Double> tl = new ThreadLocal<>();
    try {
      for (int i = 0; i < 3; i++) {
        tl.set(val);
        assertEquals(val, tl.get());
        RSThreads.clearThreadLocals(Thread.currentThread());
        assertNull(tl.get(), "All ThreadLocals should get cleared");
      }
    } finally {
      tl.remove();
    }
  }

  @Test
  public void testClearThreadLocalsNull() {
    assertThrows(NullPointerException.class, () -> RSThreads.clearThreadLocals(null),
        "NPE expected");
  }

  @Test
  public void testClearThreadLocalsWithSecurityManager() {
    RSThreads.threadLocalsField = null;
    final SecurityManager customSecurityManager = new SecurityManager() {

      @Override
      public void checkPermission(Permission perm) {
        if (perm.getName().toLowerCase().contains("access")) {
          throw new SecurityException(perm.getName());
        }
      }

    };
    System.setSecurityManager(customSecurityManager);
    try {
      final Thread currentThread = Thread.currentThread();
      assertThrows(SecurityException.class, () -> RSThreads.clearThreadLocals(currentThread));
    } finally {
      System.setSecurityManager(null);
    }
  }

  @Test
  public void testThreadDumpWorks() {
    final Lock lock = new ReentrantLock();
    for (int i = 0; i < 3; i++) {
      RSExecutors.threadPerTaskExecutor(true).execute(() -> {
        lock.lock();
        try {
          Thread.sleep(250);
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
          lock.unlock();
        }
      });
    }
    final String fullThreadDump = RSThreads.fullThreadDump();
    assertTrue(fullThreadDump.length() > 128);
  }

}

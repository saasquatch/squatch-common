package saasquatch.common.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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
    try {
      RSThreads.clearThreadLocals(null);
      fail("NPE expected");
    } catch (NullPointerException expected) {}
  }

  @Test
  public void testThreadDumpWorks() {
    final String fullThreadDump = RSThreads.fullThreadDump();
    assertTrue(fullThreadDump.length() > 128);
  }

}

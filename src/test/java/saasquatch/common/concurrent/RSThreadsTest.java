package saasquatch.common.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RSThreadsTest {

  @Test
  public void testClearThreadLocals() {
    final String val = "foo";
    final ThreadLocal<String> tl = new ThreadLocal<>();
    try {
      tl.set(val);
      assertEquals(val, tl.get());
      RSThreads.clearThreadLocals(Thread.currentThread());
      assertNull("All ThreadLocals should get cleared", tl.get());
    } finally {
      tl.remove();
    }
  }

  @Test
  public void testThreadDumpWorks() {
    final String fullThreadDump = RSThreads.fullThreadDump();
    assertTrue(fullThreadDump.length() > 128);
  }

}
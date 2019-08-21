package saasquatch.common.concurrent;

import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import saasquatch.common.base.RSThrowables;

public class RSThreadPools {
  private static final Logger logger = LoggerFactory.getLogger(RSThreadPools.class);

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

}

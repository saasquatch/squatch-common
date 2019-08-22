package saasquatch.common.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.junit.Test;

public class RSThrowablesTests {

  @Test
  public void testBasicCauseChain() {
    final Exception e0 = new Exception("0");
    final Exception e1 = new Exception("1", e0);
    final Exception e2 = new Exception("2", e1);
    final Exception e3 = new Exception("3", e2);
    final Exception e4 = new Exception("4", e3);
    final Exception e5 = new Exception("5", e4);
    final List<Throwable> basicCauseChain = RSThrowables.getCauseChainList(e5);
    assertEquals(Arrays.asList(e5, e4, e3, e2, e1, e0), basicCauseChain);
  }

  @Test
  public void testCauseChainLimit() {
    final FakeCauseRuntimeException fakeException = new FakeCauseRuntimeException();
    // We shouldn't get an infinite loop
    final List<Throwable> causeChain = RSThrowables.getCauseChainList(fakeException);
    assertEquals(RSThrowables.DEFAULT_CAUSE_CHAIN_LIMIT, causeChain.size());
  }

  @Test
  public void testLargeLimit() {
    final FakeCauseRuntimeException fakeException = new FakeCauseRuntimeException();
    final long count = RSThrowables.getCauseChainStream(fakeException, 100_000).count();
    assertEquals(100_000, count);
  }

  @Test
  public void testUnwrapAndThrow() {
    final String msg = "fake message";
    final Runnable throwingRunnable = () -> {
      throw new RuntimeException(
          new CompletionException(new UncheckedIOException(new IOException(msg))));
    };
    try {
      throwingRunnable.run();
    } catch (RuntimeException e) {
      try {
        RSThrowables.unwrapAndThrow(e, IOException.class);
        fail("IOException not thrown");
      } catch (IOException expected) {
        assertEquals(msg, expected.getMessage());
      }
    }
  }

  /**
   * A fake Exception type where {@link #getCause()} always returns a new Exception.
   * @author sli
   */
  static class FakeCauseRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized Throwable getCause() {
      /*
       * Throwable does not implement equals, which means if we create a new instance every time,
       * this.equals(this.getCause()) will always return false.
       */
      return new FakeCauseRuntimeException();
    }

  }

}

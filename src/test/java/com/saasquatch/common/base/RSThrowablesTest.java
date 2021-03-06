package com.saasquatch.common.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import com.google.common.collect.ImmutableList;

public class RSThrowablesTest {

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
    final Iterable<Throwable> causeChainIterable = RSThrowables.getCauseChain(e5);
    assertEquals(basicCauseChain, ImmutableList.copyOf(causeChainIterable));
    {
      final Iterator<Throwable> causeChainIterator = causeChainIterable.iterator();
      for (int i = 0; i < 6; i++) {
        causeChainIterator.next(); // This shouldn't error
      }
      assertThrows(NoSuchElementException.class, causeChainIterator::next);
    }
    final List<Throwable> causeChainListFromStream =
        RSThrowables.getCauseChainStream(e5).collect(Collectors.toList());
    assertEquals(basicCauseChain, causeChainListFromStream);
  }

  @Test
  public void testSingletonCauseChain() {
    final Exception e = new IOException();
    final List<Throwable> causeChain = RSThrowables.getCauseChainList(e);
    assertEquals(Collections.singletonList(e), causeChain);
  }

  @Test
  public void testListImmutablility() {
    final List<Throwable> causeChain = RSThrowables.getCauseChainList(new AssertionError());
    assertThrows(UnsupportedOperationException.class, causeChain::clear,
        "The List should be immutable");
  }

  @Test
  public void testNegativeLimit() {
    assertThrows(IllegalArgumentException.class,
        () -> RSThrowables.getCauseChainList(new Exception(), -1), "negative limit should error");
  }

  @Test
  public void testCauseChainLimit() {
    final FakeCauseRuntimeException fakeException = new FakeCauseRuntimeException();
    {
      // We shouldn't get an infinite loop
      final List<Throwable> causeChain = RSThrowables.getCauseChainList(fakeException);
      assertEquals(RSThrowables.DEFAULT_CAUSE_CHAIN_LIMIT, causeChain.size());
    }
    for (int i = 1; i < 128; i++) {
      assertEquals(i, RSThrowables.getCauseChainList(fakeException, i).size());
    }
    {
      final Iterator<Throwable> causeChain = RSThrowables.getCauseChain(fakeException, 3).iterator();
      // The following next() calls shouldn't throw an Exception
      causeChain.next();
      causeChain.next();
      causeChain.next();
      assertThrows(NoSuchElementException.class, causeChain::next);
    }
  }

  @Test
  public void testLargeLimit() {
    final int largeLimit = 100_000;
    final FakeCauseRuntimeException fakeException = new FakeCauseRuntimeException();
    final int count = (int) RSThrowables.getCauseChainStream(fakeException, largeLimit).count();
    assertEquals(largeLimit, count);
  }

  @Test
  public void testUnlimited() {
    final int largeLimit = 100_000;
    final FakeCauseRuntimeException fakeException = new FakeCauseRuntimeException();
    final int count =
        (int) RSThrowables.getCauseChainStream(fakeException, 0).limit(largeLimit).count();
    assertEquals(largeLimit, count);
  }

  @Test
  public void testCauseChainWithNull() {
    assertThrows(NullPointerException.class, () -> RSThrowables.getCauseChain(null));
  }

  @Test
  public void testBasicFindFirstInCauseChain() {
    final Exception e = new Exception(new IOException("0", new Exception(new IOException("1"))));
    assertEquals("0", RSThrowables.findFirstInCauseChain(e, IOException.class).get().getMessage());
    assertFalse(RSThrowables.findFirstInCauseChain(e, CompletionException.class).isPresent());
  }

  @Test
  public void testSelfCause() {
    final SelfCauseRuntimeException fakeException = new SelfCauseRuntimeException();
    assertEquals(Arrays.asList(fakeException), RSThrowables.getCauseChainList(fakeException, 0));
  }

  @Test
  public void testCauseEquals() {
    final FakeCauseAlwaysEqualsRuntimeException fakeException =
        new FakeCauseAlwaysEqualsRuntimeException();
    assertEquals(Arrays.asList(fakeException), RSThrowables.getCauseChainList(fakeException, 0));
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
      fail();
    } catch (RuntimeException e) {
      try {
        RSThrowables.unwrapAndThrow(e, IOException.class);
        fail("IOException not thrown");
      } catch (IOException expected) {
        assertEquals(msg, expected.getMessage());
      }
    }
    try {
      throwingRunnable.run();
      fail();
    } catch (RuntimeException e) {
      try {
        RSThrowables.unwrapAndThrow(e, ExecutionException.class);
        // The line above shouldn't do anything
      } catch (ExecutionException executionException) {
        fail("Unexpected exception: " + executionException);
      }
    }
  }

  @Test
  public void testWrapAndThrow() {
    final RuntimeException runtimeException = new UncheckedIOException(new IOException("foo"));
    try {
      RSThrowables.wrapAndThrow(runtimeException);
      fail();
    } catch (Throwable t) {
      assertSame(runtimeException, t, "We should get back the exact same RuntimeException");
    }
    final Error error = new AssertionError("foo", new ParseException("foo", 0));
    try {
      RSThrowables.wrapAndThrow(error);
      fail();
    } catch (Throwable t) {
      assertSame(error, t, "We should get back the exact same Error");
    }
    final IOException ioException = new IOException("foo", new Error());
    try {
      RSThrowables.wrapAndThrow(ioException);
      fail();
    } catch (Throwable t) {
      assertTrue(t instanceof UncheckedIOException, "We should get back an UncheckedIOException");
      assertSame(ioException, t.getCause(), "The cause should be the exact same IOException");
    }
    final ParseException parseException = new ParseException("foo", 0);
    try {
      RSThrowables.wrapAndThrow(parseException);
      fail();
    } catch (Throwable t) {
      assertTrue(t instanceof UndeclaredThrowableException);
      assertSame(parseException, t.getCause());
    }
  }

  @Test
  public void testNull() {
    assertThrows(NullPointerException.class, () -> RSThrowables.wrapAndThrow(null));
    assertThrows(NullPointerException.class,
        () -> RSThrowables.findFirstInCauseChain(null, Exception.class));
    assertThrows(NullPointerException.class,
        () -> RSThrowables.findFirstInCauseChain(new IllegalArgumentException(), null));
  }

  @Test
  public void testInterruptedException() {
    final FutureTask<Object> futureTask = new FutureTask<>(() -> {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        RSThrowables.wrapAndThrow(e);
      }
      return null;
    });
    final Thread thread = new Thread(futureTask);
    thread.setDaemon(true);
    thread.start();
    thread.interrupt();
    try {
      futureTask.get();
      fail();
    } catch (InterruptedException e) {
      fail(e);
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof UndeclaredThrowableException);
      assertTrue(e.getCause().getCause() instanceof InterruptedException);
    }
  }

  /**
   * A fake Exception type where {@link #getCause()} always returns a new Exception to emulate an
   * infinite loop.
   *
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

  /**
   * A fake Exception type where {@link #getCause()} always returns itself.
   *
   * @author sli
   */
  static class SelfCauseRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized Throwable getCause() {
      return this;
    }

  }

  static class FakeCauseAlwaysEqualsRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      return true;
    }

    @Override
    public synchronized Throwable getCause() {
      return new FakeCauseAlwaysEqualsRuntimeException();
    }

  }

}

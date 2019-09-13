package saasquatch.common.concurrent;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;

class ExecutorExecutorService extends AbstractExecutorService {

  private final Lock lock = new ReentrantLock();
  private final Condition awaitTerminationCondition = lock.newCondition();
  private int runningTaskCount = 0;
  private boolean shutdown = false;

  private final Executor executor;

  ExecutorExecutorService(@Nonnull Executor executor) {
    this.executor = Objects.requireNonNull(executor);
  }

  @Override
  public void execute(Runnable command) {
    beforeExecute();
    executor.execute(() -> {
      try {
        command.run();
      } finally {
        afterExecute();
      }
    });
  }

  private void beforeExecute() {
    lock.lock();
    try {
      if (shutdown) {
        throw new RejectedExecutionException();
      }
      runningTaskCount++;
    } finally {
      lock.unlock();
    }
  }

  private void afterExecute() {
    lock.lock();
    try {
      if (--runningTaskCount == 0) {
        awaitTerminationCondition.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void shutdown() {
    lock.lock();
    try {
      shutdown = true;
      if (runningTaskCount == 0) {
        awaitTerminationCondition.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown();
    return Collections.emptyList();
  }

  @Override
  public boolean isShutdown() {
    lock.lock();
    try {
      return shutdown;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean isTerminated() {
    lock.lock();
    try {
      return _isTerminated();
    } finally {
      lock.unlock();
    }
  }

  private boolean _isTerminated() {
    return shutdown && runningTaskCount == 0;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    lock.lock();
    try {
      for (;;) {
        if (_isTerminated()) {
          return true;
        } else if (nanos <= 0) {
          return false;
        } else {
          nanos = awaitTerminationCondition.awaitNanos(nanos);
        }
      }
    } finally {
      lock.unlock();
    }
  }

}

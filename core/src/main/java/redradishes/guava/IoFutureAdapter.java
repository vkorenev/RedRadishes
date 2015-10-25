package redradishes.guava;

import com.google.common.util.concurrent.ListenableFuture;
import org.xnio.IoFuture;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class IoFutureAdapter<T> implements ListenableFuture<T> {
  private final IoFuture<T> ioFuture;

  public IoFutureAdapter(IoFuture<T> ioFuture) {
    this.ioFuture = ioFuture;
  }

  @Override
  public void addListener(Runnable listener, Executor executor) {
    ioFuture.addNotifier((ioFuture, attachment) -> executor.execute(listener), null);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    ioFuture.cancel();
    return ioFuture.getStatus() != IoFuture.Status.DONE;
  }

  @Override
  public boolean isCancelled() {
    return ioFuture.getStatus() == IoFuture.Status.CANCELLED;
  }

  @Override
  public boolean isDone() {
    return ioFuture.getStatus() == IoFuture.Status.DONE;
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    try {
      return ioFuture.getInterruptibly();
    } catch (IOException e) {
      throw new ExecutionException(e);
    }
  }

  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    switch (ioFuture.awaitInterruptibly(timeout, unit)) {
      case WAITING:
        throw new TimeoutException();
      case CANCELLED:
        throw new CancellationException();
      case FAILED:
        throw new ExecutionException(ioFuture.getException());
      default:
        try {
          return ioFuture.getInterruptibly();
        } catch (IOException e) {
          throw new ExecutionException(e);
        }
    }
  }
}

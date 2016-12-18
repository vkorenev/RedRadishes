package redradishes.guava;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.xnio.ByteBufferPool;
import org.xnio.XnioWorker;
import redradishes.Request;
import redradishes.XnioRedisClient;
import redradishes.commands.Command1;
import redradishes.commands.Command2;
import redradishes.commands.Command3;

import java.net.SocketAddress;
import java.nio.charset.Charset;

public class RedisClient extends XnioRedisClient<ListenableFuture, SettableFuture> {
  RedisClient(XnioWorker worker, SocketAddress address, ByteBufferPool bufferPool, Charset charset) {
    super(worker, address, bufferPool, charset);
  }

  @Override
  protected ListenableFuture createCancelledFuture() {
    return Futures.immediateCancelledFuture();
  }

  @Override
  protected ListenableFuture createFailedFuture(Throwable exception) {
    return Futures.immediateFailedFuture(exception);
  }

  @Override
  protected SettableFuture createFuture() {
    return SettableFuture.create();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <T> void complete(SettableFuture future, T value) {
    future.set(value);
  }

  @Override
  protected void completeExceptionally(SettableFuture future, Throwable exception) {
    future.setException(exception);
  }

  @Override
  protected void cancel(SettableFuture future) {
    future.cancel(true);
  }

  @SuppressWarnings("unchecked")
  public <T> ListenableFuture<T> send(Request<T> request) {
    return send_(request);
  }

  public final <T, R> ListenableFuture<R> send(Command1<T, R> command, T arg) {
    return send(command.apply(arg));
  }

  public final <T1, T2, R> ListenableFuture<R> send(Command2<T1, T2, R> command, T1 arg1, T2 arg2) {
    return send(command.apply(arg1, arg2));
  }

  public final <T1, T2, T3, R> ListenableFuture<R> send(Command3<T1, T2, T3, R> command, T1 arg1, T2 arg2, T3 arg3) {
    return send(command.apply(arg1, arg2, arg3));
  }

  @SafeVarargs
  public final <E, R> ListenableFuture<R> send(Command1<E[], R> command, E... arg1) {
    return send(command.apply(arg1));
  }

  @SafeVarargs
  public final <T, E, R> ListenableFuture<R> send(Command2<T, E[], R> command, T arg1, E... arg2) {
    return send(command.apply(arg1, arg2));
  }
}

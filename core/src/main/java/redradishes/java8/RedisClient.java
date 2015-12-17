package redradishes.java8;

import org.xnio.Pool;
import org.xnio.XnioWorker;
import redradishes.Request;
import redradishes.XnioRedisClient;
import redradishes.commands.Command1;
import redradishes.commands.Command2;
import redradishes.commands.Command3;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class RedisClient extends XnioRedisClient<CompletableFuture, CompletableFuture> {
  RedisClient(XnioWorker worker, SocketAddress address, Pool<ByteBuffer> bufferPool, Charset charset) {
    super(worker, address, bufferPool, charset);
  }

  @Override
  protected CompletableFuture createCancelledFuture() {
    CompletableFuture future = new CompletableFuture();
    future.completeExceptionally(new CancellationException());
    return future;
  }

  @Override
  protected CompletableFuture createFailedFuture(Throwable exception) {
    CompletableFuture future = new CompletableFuture();
    future.completeExceptionally(exception);
    return future;
  }

  @Override
  protected CompletableFuture createFuture() {
    return new CompletableFuture();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <T> void complete(CompletableFuture future, T value) {
    future.complete(value);
  }

  @Override
  protected void completeExceptionally(CompletableFuture future, Throwable exception) {
    future.completeExceptionally(exception);
  }

  @Override
  protected void cancel(CompletableFuture future) {
    future.cancel(true);
  }

  @SuppressWarnings("unchecked")
  public <T> CompletableFuture<T> send(Request<T> request) {
    return send_(request);
  }

  public final <T, R> CompletableFuture<R> send(Command1<T, R> command, T arg) {
    return send(command.apply(arg));
  }

  public final <T1, T2, R> CompletableFuture<R> send(Command2<T1, T2, R> command, T1 arg1, T2 arg2) {
    return send(command.apply(arg1, arg2));
  }

  public final <T1, T2, T3, R> CompletableFuture<R> send(Command3<T1, T2, T3, R> command, T1 arg1, T2 arg2, T3 arg3) {
    return send(command.apply(arg1, arg2, arg3));
  }

  @SafeVarargs
  public final <E, R> CompletableFuture<R> send(Command1<E[], R> command, E... arg1) {
    return send(command.apply(arg1));
  }

  @SafeVarargs
  public final <T, E, R> CompletableFuture<R> send(Command2<T, E[], R> command, T arg1, E... arg2) {
    return send(command.apply(arg1, arg2));
  }
}

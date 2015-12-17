package redradishes;

import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import redradishes.RedisClientConnection.CommandEncoderDecoder;
import redradishes.decoder.parser.ReplyParser;
import redradishes.encoder.ByteSink;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class XnioRedisClient<F, SF extends F> implements AutoCloseable {
  private final BlockingQueue<CommandEncoderDecoder> writerQueue = new LinkedBlockingQueue<>();
  private final IoFuture<StreamConnection> streamConnectionFuture;
  private volatile RedisClientConnection redisClientConnection;
  private volatile IOException failure;
  private volatile boolean closed = false;

  protected XnioRedisClient(XnioWorker worker, SocketAddress address, Pool<ByteBuffer> bufferPool, Charset charset) {
    this.streamConnectionFuture = worker.openStreamConnection(address, null, OptionMap.EMPTY);
    this.streamConnectionFuture.addNotifier(new IoFuture.HandlingNotifier<StreamConnection, Void>() {
      @Override
      public void handleFailed(IOException exception, Void v) {
        failure = exception;
        CommandEncoderDecoder commandEncoderDecoder;
        while ((commandEncoderDecoder = writerQueue.poll()) != null) {
          commandEncoderDecoder.fail(failure);
        }
      }

      @Override
      public void handleDone(StreamConnection connection, Void v) {
        redisClientConnection = new RedisClientConnection(connection, bufferPool, charset, writerQueue);
        if (!writerQueue.isEmpty()) {
          redisClientConnection.commandAdded();
        }
      }
    }, null);
  }

  public <T> F send_(final Request<T> request) {
    if (closed) {
      return createCancelledFuture();
    }
    if (failure != null) {
      return createFailedFuture(failure);
    }
    final SF future = createFuture();
    writerQueue.add(new CommandEncoderDecoder() {
      private ReplyParser<? extends T> parser = request.parser();

      @Override
      public void writeTo(ByteSink sink) {
        request.writeTo(sink);
      }

      @Override
      public boolean parse(ByteBuffer buffer, CharsetDecoder charsetDecoder) throws IOException {
        return parser.parseReply(buffer, value -> {
          complete(future, value);
          return true;
        }, partial -> {
          parser = partial;
          return false;
        }, exception -> {
          completeExceptionally(future, exception);
          return true;
        }, charsetDecoder);
      }

      @Override
      public void fail(Throwable e) {
        completeExceptionally(future, e);
      }

      @Override
      public void cancel() {
        XnioRedisClient.this.cancel(future);
      }
    });
    if (redisClientConnection != null) {
      redisClientConnection.commandAdded();
    }
    return future;
  }

  protected abstract F createCancelledFuture();

  protected abstract F createFailedFuture(Throwable exception);

  protected abstract SF createFuture();

  protected abstract <T> void complete(SF future, T value);

  protected abstract void completeExceptionally(SF future, Throwable exception);

  protected abstract void cancel(SF future);

  @Override
  public void close() {
    closed = true;
    IoUtils.safeClose(streamConnectionFuture);
  }
}

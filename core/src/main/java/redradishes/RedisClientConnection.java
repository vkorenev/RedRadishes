package redradishes;

import com.google.common.base.Throwables;
import org.xnio.ByteBufferPool;
import org.xnio.IoUtils;
import org.xnio.StreamConnection;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;
import redradishes.encoder.ByteSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.xnio.channels.Channels.resumeWritesAsync;

class RedisClientConnection {
  private final BlockingQueue<ReplyDecoder> decoderQueue = new LinkedBlockingQueue<>();
  private final StreamSinkChannel sinkChannel;
  private ReplyDecoder currentDecoder;

  RedisClientConnection(StreamConnection connection, ByteBufferPool bufferPool, Charset charset,
      BlockingQueue<CommandEncoderDecoder> commandsQueue) {
    CharsetDecoder charsetDecoder = charset.newDecoder();
    StreamSourceChannel sourceChannel = connection.getSourceChannel();
    this.sinkChannel = connection.getSinkChannel();
    sourceChannel.getReadSetter().set(inChannel -> {
      ByteBuffer readBuffer = bufferPool.allocate();
      try {
        while (inChannel.read(readBuffer) > 0) {
          readBuffer.flip();
          try {
            while (readBuffer.hasRemaining()) {
              if (decoder().parse(readBuffer, charsetDecoder)) {
                currentDecoder = null;
              }
            }
          } finally {
            readBuffer.clear();
          }
        }
      } catch (Throwable e) {
        IoUtils.safeClose(sinkChannel);
        IoUtils.safeClose(inChannel);
        failUnfinished(e);
      } finally {
        ByteBufferPool.free(readBuffer);
      }
    });
    sourceChannel.getCloseSetter().set(inChannel -> {
      IoUtils.safeClose(sinkChannel);
      failUnfinished(new IOException("Server closed connection"));
    });
    sourceChannel.resumeReads();
    ByteBufferBundle byteBufferBundle = new ByteBufferBundle(bufferPool);
    this.sinkChannel.getWriteSetter().set(outChannel -> {
      try {
        while (!commandsQueue.isEmpty() || !byteBufferBundle.isEmpty()) {
          ByteSink sink = new ByteBufferSink(byteBufferBundle);
          CommandEncoderDecoder command;
          while (byteBufferBundle.allocSize() <= 1 && (command = commandsQueue.poll()) != null) {
            decoderQueue.add(command);
            command.writeTo(sink);
          }
          long bytesWritten = byteBufferBundle.writeTo(outChannel);
          if (bytesWritten == 0) {
            return;
          }
        }
      } catch (Throwable e) {
        IoUtils.safeClose(sinkChannel);
        failUnfinished(e);
      }
      outChannel.suspendWrites();
    });
  }

  private void failUnfinished(Throwable e) {
    if (currentDecoder != null) {
      currentDecoder.fail(e);
      currentDecoder = null;
    }
    ReplyDecoder decoder;
    while ((decoder = decoderQueue.poll()) != null) {
      decoder.fail(e);
    }
  }

  private ReplyDecoder decoder() {
    if (currentDecoder == null) {
      currentDecoder = decoderQueue.poll();
      if (currentDecoder == null) {
        currentDecoder = new ReplyDecoder() {
          @Override
          public boolean parse(ByteBuffer buffer, CharsetDecoder charsetDecoder) throws IOException {
            int len = buffer.remaining();
            byte[] bytes = new byte[len];
            buffer.get(bytes);
            throw new IllegalStateException("Unexpected input: " + Arrays.toString(bytes));
          }

          @Override
          public void fail(Throwable e) {
            throw Throwables.propagate(e);
          }

          @Override
          public void cancel() {
          }
        };
      }
    }
    return currentDecoder;
  }

  void commandAdded() {
    resumeWritesAsync(sinkChannel);
  }

  public void close() {
    if (currentDecoder != null) {
      currentDecoder.cancel();
    }
    decoderQueue.forEach(ReplyDecoder::cancel);
  }

  interface ReplyDecoder {
    boolean parse(ByteBuffer buffer, CharsetDecoder charsetDecoder) throws IOException;

    void fail(Throwable e);

    void cancel();
  }

  interface CommandEncoderDecoder extends ReplyDecoder {
    void writeTo(ByteSink sink);
  }
}

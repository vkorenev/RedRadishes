package redradishes;

import com.google.common.base.Throwables;
import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.StreamConnection;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;
import redradishes.encoder.ByteSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class RedisClientConnection {
  private final BlockingQueue<ReplyDecoder> decoderQueue = new LinkedBlockingQueue<>();
  private final StreamSinkChannel outChannel;
  private ReplyDecoder currentDecoder;

  RedisClientConnection(StreamConnection connection, Pool<ByteBuffer> bufferPool, Charset charset,
      BlockingQueue<CommandEncoderDecoder> commandsQueue) {
    CharsetDecoder charsetDecoder = charset.newDecoder();
    StreamSourceChannel sourceChannel = connection.getSourceChannel();
    sourceChannel.getReadSetter().set(inChannel -> {
      try (Pooled<ByteBuffer> pooledByteBuffer = bufferPool.allocate()) {
        ByteBuffer readBuffer = pooledByteBuffer.getResource();
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
        if (currentDecoder != null) {
          currentDecoder.fail(e);
        }
        decoderQueue.forEach(decoder -> decoder.fail(e));
      }
    });
    sourceChannel.resumeReads();
    ByteBufferBundle byteBufferBundle = new ByteBufferBundle(bufferPool);
    CharsetEncoder charsetEncoder = charset.newEncoder();
    this.outChannel = connection.getSinkChannel();
    this.outChannel.getWriteSetter().set(outChannel -> {
      try {
        while (!commandsQueue.isEmpty() || !byteBufferBundle.isEmpty()) {
          ByteSink sink = new ByteBufferSink(byteBufferBundle, charsetEncoder);
          CommandEncoderDecoder command;
          while (byteBufferBundle.allocSize() <= 1 && (command = commandsQueue.poll()) != null) {
            decoderQueue.add(command);
            command.writeTo(sink);
          }
          byteBufferBundle.startReading();
          try {
            long bytesWritten = outChannel.write(byteBufferBundle.getReadBuffers());
            if (bytesWritten == 0) {
              return;
            }
          } finally {
            byteBufferBundle.startWriting();
          }
        }
      } catch (IOException e) {
        if (currentDecoder != null) {
          currentDecoder.fail(e);
        }
        decoderQueue.forEach(decoder -> decoder.fail(e));
      }
      outChannel.suspendWrites();
    });
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
    outChannel.resumeWrites();
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

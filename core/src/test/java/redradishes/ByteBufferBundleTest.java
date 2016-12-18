package redradishes;

import com.google.common.primitives.Bytes;
import com.pholser.junit.quickcheck.ForAll;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.contrib.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.xnio.Buffers;
import org.xnio.ByteBufferPool;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Integer.min;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

@RunWith(Theories.class)
public class ByteBufferBundleTest {
  @Theory
  public void writes(@ForAll byte[][] messages, @TestedOn(ints = {7, 10}) int writeChunk,
      @TestedOn(ints = {7, 10, 13}) int readChunk) throws IOException {
    List<byte[]> receivedMessages = new ArrayList<>();

    ByteBufferPool byteBufferPool = ByteBufferPool.SMALL_DIRECT;
    ByteBufferBundle byteBufferBundle = new ByteBufferBundle(byteBufferPool);
    for (byte[] message : messages) {
      write(message, byteBufferBundle, writeChunk);

      byteBufferBundle.writeTo((SimpleGatheringByteChannel) (srcs, offset, length) -> {
        int remaining = remaining(srcs, offset, length);
        if (remaining > readChunk) {
          return read(srcs, offset, length, receivedMessages::add, readChunk);
        } else {
          return 0;
        }
      });
    }

    byteBufferBundle.writeTo(
        (SimpleGatheringByteChannel) (srcs, offset, length) -> read(srcs, offset, length, receivedMessages::add,
            remaining(srcs, offset, length)));

    assertThat(Bytes.concat(messages), equalTo(Bytes.concat(receivedMessages.stream().toArray(byte[][]::new))));
    assertTrue(byteBufferBundle.isEmpty());
  }

  private int remaining(ByteBuffer[] readBuffers, int offset, int length) {
    return Arrays.stream(readBuffers, offset, offset + length).mapToInt(Buffer::remaining).sum();
  }

  private int read(ByteBuffer[] readBuffers, int offset, int length, Consumer<byte[]> consumer, int readLen) {
    byte[] dest = new byte[readLen];
    int copied = Buffers.copy(ByteBuffer.wrap(dest), readBuffers, offset, length);
    consumer.accept(dest);
    return copied;
  }

  private void write(byte[] message, ByteBufferBundle byteBufferBundle, int writeChunk) {
    int bytesWritten = 0;
    ByteBuffer buffer = byteBufferBundle.get();
    int remaining;
    while ((remaining = message.length - bytesWritten) > 0) {
      int writeLen = min(remaining, writeChunk);
      if (buffer.remaining() < writeLen) {
        buffer = byteBufferBundle.getNew();
      }
      buffer.put(message, bytesWritten, writeLen);
      bytesWritten += writeLen;
    }
  }

  private interface SimpleGatheringByteChannel extends GatheringByteChannel {
    @Override
    default long write(ByteBuffer[] srcs) throws IOException {
      return write(srcs, 0, srcs.length);
    }

    @Override
    default int write(ByteBuffer src) throws IOException {
      return (int) write(new ByteBuffer[]{src});
    }

    @Override
    default boolean isOpen() {
      return true;
    }

    @Override
    default void close() throws IOException {
    }
  }
}

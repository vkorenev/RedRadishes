package redradishes;

import com.google.common.primitives.Bytes;
import com.pholser.junit.quickcheck.ForAll;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.contrib.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.xnio.Buffers;
import org.xnio.ByteBufferSlicePool;
import org.xnio.Pool;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Integer.min;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.xnio.BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR;

@RunWith(Theories.class)
public class ByteBufferBundleTest {
  @Theory
  public void writes(@ForAll byte[][] messages, @TestedOn(ints = {7, 10}) int writeChunk,
      @TestedOn(ints = {7, 10, 13}) int readChunk) {
    List<byte[]> receivedMessages = new ArrayList<>();

    Pool<ByteBuffer> byteBufferPool = new ByteBufferSlicePool(DIRECT_BYTE_BUFFER_ALLOCATOR, 10, 1000);
    ByteBufferBundle byteBufferBundle = new ByteBufferBundle(byteBufferPool);
    for (byte[] message : messages) {
      write(message, byteBufferBundle, writeChunk);

      byteBufferBundle.startReading();
      try {
        ByteBuffer[] readBuffers = byteBufferBundle.getReadBuffers();
        int remaining = remaining(readBuffers);
        if (remaining > readChunk) {
          read(readBuffers, receivedMessages::add, readChunk);
        }
      } finally {
        byteBufferBundle.startWriting();
      }
    }

    byteBufferBundle.startReading();
    try {
      ByteBuffer[] readBuffers = byteBufferBundle.getReadBuffers();
      read(readBuffers, receivedMessages::add, remaining(readBuffers));
    } finally {
      byteBufferBundle.startWriting();
    }

    assertThat(Bytes.concat(messages), equalTo(Bytes.concat(receivedMessages.stream().toArray(byte[][]::new))));
    assertThat(byteBufferBundle.getReadBuffers(), emptyArray());
  }

  private int remaining(ByteBuffer[] readBuffers) {
    return Arrays.stream(readBuffers).mapToInt(Buffer::remaining).sum();
  }

  private void read(ByteBuffer[] readBuffers, Consumer<byte[]> consumer, int readLen) {
    byte[] dest = new byte[readLen];
    Buffers.copy(ByteBuffer.wrap(dest), readBuffers, 0, readBuffers.length);
    consumer.accept(dest);
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
}

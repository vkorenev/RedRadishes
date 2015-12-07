package redradishes;

import org.xnio.Pool;
import org.xnio.Pooled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

class ByteBufferBundle {
  private final Pool<ByteBuffer> pool;
  private final Deque<Pooled<ByteBuffer>> allocated = new LinkedList<>();
  private ByteBuffer currentWriteBuffer = null;

  ByteBufferBundle(Pool<ByteBuffer> pool) {
    this.pool = pool;
  }

  ByteBuffer get() {
    if (currentWriteBuffer != null) {
      if (currentWriteBuffer.hasRemaining()) {
        return currentWriteBuffer;
      } else {
        currentWriteBuffer.flip();
      }
    }
    currentWriteBuffer = allocateBuffer();
    return currentWriteBuffer;
  }

  ByteBuffer getNew() {
    if (currentWriteBuffer != null) {
      currentWriteBuffer.flip();
    }
    currentWriteBuffer = allocateBuffer();
    return currentWriteBuffer;
  }

  private ByteBuffer allocateBuffer() {
    Pooled<ByteBuffer> pooledBuffer = pool.allocate();
    allocated.add(pooledBuffer);
    return pooledBuffer.getResource();
  }

  long writeTo(GatheringByteChannel channel) throws IOException {
    startReading();
    try {
      return channel.write(getReadBuffers());
    } finally {
      startWriting();
    }
  }

  private void startReading() {
    if (currentWriteBuffer != null) {
      currentWriteBuffer.flip();
    }
    currentWriteBuffer = null;
  }

  private ByteBuffer[] getReadBuffers() {
    return allocated.stream().map(Pooled::getResource).toArray(ByteBuffer[]::new);
  }

  private void startWriting() {
    Iterator<Pooled<ByteBuffer>> iterator = allocated.iterator();
    while (iterator.hasNext()) {
      Pooled<ByteBuffer> pooledBuffer = iterator.next();
      ByteBuffer byteBuffer = pooledBuffer.getResource();
      if (!byteBuffer.hasRemaining()) {
        byteBuffer.clear();
        pooledBuffer.free();
        iterator.remove();
      } else {
        break;
      }
    }
    Pooled<ByteBuffer> lastPooledBuffer = allocated.peekLast();
    if (lastPooledBuffer != null) {
      ByteBuffer lastBuffer = lastPooledBuffer.getResource();
      if (lastBuffer.limit() < lastBuffer.capacity()) {
        currentWriteBuffer = lastBuffer.compact();
      }
    }
  }

  boolean isEmpty() {
    return allocated.isEmpty();
  }

  int allocSize() {
    return allocated.size();
  }
}

package redradishes;

import org.xnio.ByteBufferPool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

class ByteBufferBundle {
  private final ByteBufferPool pool;
  private final Deque<ByteBuffer> allocated = new LinkedList<>();
  private ByteBuffer currentWriteBuffer = null;

  ByteBufferBundle(ByteBufferPool pool) {
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
    ByteBuffer pooledBuffer = pool.allocate();
    allocated.add(pooledBuffer);
    return pooledBuffer;
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
    return allocated.toArray(new ByteBuffer[allocated.size()]);
  }

  private void startWriting() {
    Iterator<ByteBuffer> iterator = allocated.iterator();
    while (iterator.hasNext()) {
      ByteBuffer byteBuffer = iterator.next();
      if (!byteBuffer.hasRemaining()) {
        byteBuffer.clear();
        ByteBufferPool.free(byteBuffer);
        iterator.remove();
      } else {
        break;
      }
    }
    ByteBuffer lastBuffer = allocated.peekLast();
    if (lastBuffer != null) {
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

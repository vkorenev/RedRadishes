package redradishes.java8;

import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class RedisClientFactory implements AutoCloseable {
  private final Pool<ByteBuffer> byteBufferPool =
      new ByteBufferSlicePool(BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR, 4096, 4096 * 256);
  private final Charset charset;
  private final XnioWorker worker;

  public RedisClientFactory(Charset charset, int ioThreads) throws IOException {
    Xnio xnio = Xnio.getInstance();
    this.charset = charset;
    worker = xnio.createWorker(OptionMap.create(Options.WORKER_IO_THREADS, ioThreads));
  }

  public RedisClient connect(SocketAddress address) {
    return new RedisClient(worker, address, byteBufferPool, charset);
  }

  @Override
  public void close() {
    worker.shutdown();
  }
}

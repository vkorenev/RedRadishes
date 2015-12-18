package redradishes.decoder;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

public class TestBulkStringBuilderFactory implements SimpleBulkStringBuilderFactory<byte[]> {
  @Override
  public Builder<byte[]> create(int length, CharsetDecoder charsetDecoder) {
    return new Builder<byte[]>() {
      private final byte[] bytes = new byte[length];
      private int offset = 0;
      private boolean finalized = false;

      @Override
      public Builder<byte[]> append(ByteBuffer buffer) {
        if (finalized) throw new IllegalStateException();
        int len = buffer.remaining();
        buffer.get(bytes, offset, len);
        offset += len;
        return this;
      }

      @Override
      public byte[] build() {
        finalized = true;
        return bytes;
      }
    };
  }
}

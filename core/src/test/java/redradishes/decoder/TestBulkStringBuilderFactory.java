package redradishes.decoder;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

public class TestBulkStringBuilderFactory implements BulkStringBuilderFactory<byte[]> {
  @Override
  public Builder<byte[]> create(int length, CharsetDecoder charsetDecoder) {
    return new Builder<byte[]>() {
      private final byte[] bytes = new byte[length];
      private int offset = 0;
      private boolean finalized = false;

      @Override
      public void append(ByteBuffer buffer, CharsetDecoder charsetDecoder) {
        if (finalized) throw new IllegalStateException();
        int len = buffer.remaining();
        buffer.get(bytes, offset, len);
        offset += len;
      }

      @Override
      public byte[] appendLast(ByteBuffer buffer, CharsetDecoder charsetDecoder) {
        append(buffer, charsetDecoder);
        finalized = true;
        return bytes;
      }
    };
  }
}

package redradishes.decoder;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;

interface SimpleBulkStringBuilderFactory<R>
    extends BulkStringBuilderFactory<SimpleBulkStringBuilderFactory.Builder<R>, R> {
  @Override
  default Builder<R> append(Builder<R> builder, ByteBuffer buffer, CharsetDecoder charsetDecoder) {
    return builder.append(buffer);
  }

  @Override
  default R appendLast(Builder<R> builder, ByteBuffer buffer, CharsetDecoder charsetDecoder) {
    return builder.append(buffer).build();
  }

  interface Builder<T> {
    Builder<T> append(ByteBuffer buffer);

    T build();
  }
}

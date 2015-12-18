package redradishes.decoder;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Objects;
import java.util.function.Function;

public interface BulkStringBuilderFactory<B, R> {
  B create(int length, CharsetDecoder charsetDecoder);

  B append(B builder, ByteBuffer buffer, CharsetDecoder charsetDecoder);

  R appendLast(B builder, ByteBuffer buffer, CharsetDecoder charsetDecoder);

  default <U> BulkStringBuilderFactory<B, U> map(Function<? super R, ? extends U> mapper) {
    Objects.requireNonNull(mapper);
    return new BulkStringBuilderFactory<B, U>() {
      @Override
      public B create(int length, CharsetDecoder charsetDecoder) {
        return BulkStringBuilderFactory.this.create(length, charsetDecoder);
      }

      @Override
      public B append(B builder, ByteBuffer buffer, CharsetDecoder charsetDecoder) {
        return BulkStringBuilderFactory.this.append(builder, buffer, charsetDecoder);
      }

      @Override
      public U appendLast(B builder, ByteBuffer buffer, CharsetDecoder charsetDecoder) {
        return mapper.apply(BulkStringBuilderFactory.this.appendLast(builder, buffer, charsetDecoder));
      }
    };
  }
}

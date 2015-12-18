package redradishes.decoder;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Objects;
import java.util.function.Function;

public interface BulkStringBuilderFactory<T> {
  Builder<T> create(int length, CharsetDecoder charsetDecoder);

  default <R> BulkStringBuilderFactory<R> map(Function<? super T, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return (length, charsetDecoder) -> {
      Builder<T> builder = create(length, charsetDecoder);
      return new Builder<R>() {
        @Override
        public void append(ByteBuffer buffer, CharsetDecoder charsetDecoder) {
          builder.append(buffer, charsetDecoder);
        }

        @Override
        public R appendLast(ByteBuffer buffer, CharsetDecoder charsetDecoder) {
          return mapper.apply(builder.appendLast(buffer, charsetDecoder));
        }
      };
    };
  }

  interface Builder<T> {
    void append(ByteBuffer buffer, CharsetDecoder charsetDecoder);

    T appendLast(ByteBuffer buffer, CharsetDecoder charsetDecoder);
  }
}

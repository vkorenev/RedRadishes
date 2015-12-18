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
        public void append(ByteBuffer buffer) {
          builder.append(buffer);
        }

        @Override
        public R appendLast(ByteBuffer buffer) {
          return mapper.apply(builder.appendLast(buffer));
        }
      };
    };
  }

  interface Builder<T> {
    void append(ByteBuffer buffer);

    T appendLast(ByteBuffer buffer);
  }
}

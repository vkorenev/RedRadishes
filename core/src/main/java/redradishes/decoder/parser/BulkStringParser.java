package redradishes.decoder.parser;

import redradishes.decoder.BulkStringBuilderFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

public class BulkStringParser<T, B> implements Parser<T> {
  private static final int READING = 0;
  private static final int WAITING_FOR_CR = 1;
  private static final int WAITING_FOR_LF = 2;
  private final BulkStringBuilderFactory<B, ? extends T> builderFactory;
  private final int len;

  public BulkStringParser(int len, BulkStringBuilderFactory<B, ? extends T> builderFactory) {
    this.len = len;
    this.builderFactory = builderFactory;
  }

  @Override
  public <U> U parse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialHandler<? super T, U> partialHandler, CharsetDecoder charsetDecoder) {
    return doParse(buffer, resultHandler, partialHandler, builderFactory.create(len, charsetDecoder), len, READING,
        null, charsetDecoder);
  }

  private <U> U doParse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialHandler<? super T, U> partialHandler, B builder, int len, int state, @Nullable T result,
      CharsetDecoder charsetDecoder) {
    readLoop:
    while (buffer.hasRemaining()) {
      switch (state) {
        case READING:
          int remaining = buffer.remaining();
          if (remaining >= len) {
            ByteBuffer src = buffer.slice();
            src.limit(len);
            buffer.position(buffer.position() + len);
            result = builderFactory.appendLast(builder, src, charsetDecoder);
            if (src.hasRemaining()) {
              throw new IllegalStateException("Bulk string decoding error");
            }
            state = WAITING_FOR_CR;
          } else {
            builder = builderFactory.append(builder, buffer, charsetDecoder);
            int bytesRead = remaining - buffer.remaining();
            len -= bytesRead;
            break readLoop;
          }
          break;
        case WAITING_FOR_CR:
          if (buffer.get() == '\r') {
            state = WAITING_FOR_LF;
          } else {
            throw new IllegalStateException("CR is expected");
          }
          break;
        case WAITING_FOR_LF:
          if (buffer.get() == '\n') {
            return resultHandler.apply(result);
          } else {
            throw new IllegalStateException("LF is expected");
          }
      }
    }
    B builder1 = builder;
    int len1 = len;
    int state1 = state;
    T result1 = result;
    return partialHandler.partial(new Parser<T>() {
      @Override
      public <U1> U1 parse(ByteBuffer buffer, Function<? super T, U1> resultHandler,
          PartialHandler<? super T, U1> partialHandler, CharsetDecoder charsetDecoder) {
        return doParse(buffer, resultHandler, partialHandler, builder1, len1, state1, result1, charsetDecoder);
      }
    });
  }
}

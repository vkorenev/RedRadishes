package redradishes.decoder.parser;

import redradishes.decoder.BulkStringBuilderFactory;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

public class BulkStringParser<T, B> implements ReplyParser<T> {
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
  public <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    return doParse(buffer, resultHandler, partialReplyHandler, builderFactory.create(len, charsetDecoder), len, READING,
        null, charsetDecoder);
  }

  private <U> U doParse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, B builder, int len, int state, @Nullable T result,
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
    return partialReplyHandler.partialReply(new ReplyParser<T>() {
      @Override
      public <U1> U1 parseReply(ByteBuffer buffer1, Function<? super T, U1> resultHandler1,
          PartialReplyHandler<? super T, U1> partialReplyHandler1, FailureHandler<U1> failureHandler,
          CharsetDecoder charsetDecoder1) {
        return doParse(buffer1, resultHandler1, partialReplyHandler1, builder1, len1, state1, result1, charsetDecoder1);
      }
    });
  }
}

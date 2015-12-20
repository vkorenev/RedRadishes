package redradishes.decoder.parser;

import redradishes.decoder.BulkStringBuilderFactory;
import redradishes.decoder.ReplyParseException;

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
    return doParse(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder,
        builderFactory.create(len, charsetDecoder), len, READING, null, null);
  }

  private <U> U doParse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder, B builder, int len, int state, @Nullable T result, @Nullable Exception exception) {
    readLoop:
    while (buffer.hasRemaining()) {
      switch (state) {
        case READING:
          int remaining = buffer.remaining();
          if (remaining >= len) {
            if (exception == null) {
              ByteBuffer src = buffer.slice();
              src.limit(len);
              try {
                result = builderFactory.appendLast(builder, src, charsetDecoder);
                if (src.hasRemaining()) {
                  exception = new ReplyParseException("Bulk string decoder has not consumed all input");
                }
              } catch (Exception e) {
                exception = e;
              }
            }
            buffer.position(buffer.position() + len);
            state = WAITING_FOR_CR;
          } else {
            if (exception == null) {
              try {
                builder = builderFactory.append(builder, buffer, charsetDecoder);
              } catch (Exception e) {
                exception = e;
              }
              int bytesRead = remaining - buffer.remaining();
              len -= bytesRead;
            } else {
              buffer.position(buffer.limit());
              len -= remaining;
            }
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
            return exception == null ? resultHandler.apply(result) : failureHandler.failure(exception);
          } else {
            throw new IllegalStateException("LF is expected");
          }
      }
    }
    B builder1 = builder;
    int len1 = len;
    int state1 = state;
    T result1 = result;
    Exception exception1 = exception;
    return partialReplyHandler.partialReply(new ReplyParser<T>() {
      @Override
      public <U1> U1 parseReply(ByteBuffer buffer, Function<? super T, U1> resultHandler,
          PartialReplyHandler<? super T, U1> partialReplyHandler, FailureHandler<U1> failureHandler,
          CharsetDecoder charsetDecoder) {
        return doParse(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder, builder1, len1,
            state1, result1, exception1);
      }
    });
  }
}

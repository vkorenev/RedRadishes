package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Objects;
import java.util.function.Function;

public interface ReplyParser<T> {
  <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder);

  default <R> ReplyParser<R> map(Function<? super T, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return new ReplyParser<R>() {
      @Override
      public <U> U parseReply(ByteBuffer buffer, Function<? super R, U> resultHandler,
          PartialReplyHandler<? super R, U> partialReplyHandler, FailureHandler<U> failureHandler,
          CharsetDecoder charsetDecoder) {
        return ReplyParser.this.parseReply(buffer, value -> resultHandler.apply(mapper.apply(value)),
            partial -> partialReplyHandler.partialReply(partial.map(mapper)), failureHandler, charsetDecoder);
      }
    };
  }

  interface PartialReplyHandler<T, U> extends Parser.PartialHandler<T, U> {
    U partialReply(ReplyParser<? extends T> partial);

    @Override
    default U partial(Parser<? extends T> partial) {
      return partialReply(partial);
    }
  }

  interface FailureHandler<U> {
    U failure(CharSequence message);
  }
}

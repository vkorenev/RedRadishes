package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Objects;
import java.util.function.Function;

public interface Parser<T> extends ReplyParser<T> {
  <U> U parse(ByteBuffer buffer, Function<? super T, U> resultHandler, PartialHandler<? super T, U> partialHandler,
      CharsetDecoder charsetDecoder);

  @Override
  default <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    return parse(buffer, resultHandler, partialReplyHandler, charsetDecoder);
  }

  @Override
  default <R> Parser<R> map(Function<? super T, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return new Parser<R>() {
      @Override
      public <U> U parse(ByteBuffer buffer, Function<? super R, U> resultHandler,
          PartialHandler<? super R, U> partialHandler, CharsetDecoder charsetDecoder) {
        return Parser.this.parse(buffer, value -> resultHandler.apply(mapper.apply(value)),
            partial -> partialHandler.partial(partial.map(mapper)), charsetDecoder);
      }
    };
  }

  interface PartialHandler<T, U> {
    U partial(Parser<? extends T> partial);
  }
}

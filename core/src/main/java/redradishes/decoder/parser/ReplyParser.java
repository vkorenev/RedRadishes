package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Objects;
import java.util.function.BiFunction;
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

  default <R> ReplyParser<R> fail(Function<? super T, ? extends Throwable> mapper) {
    return new ReplyParser<R>() {
      @Override
      public <U> U parseReply(ByteBuffer buffer, Function<? super R, U> resultHandler,
          PartialReplyHandler<? super R, U> partialReplyHandler, FailureHandler<U> failureHandler,
          CharsetDecoder charsetDecoder) {
        return ReplyParser.this.parseReply(buffer, value -> failureHandler.failure(mapper.apply(value)),
            partial -> partialReplyHandler.partialReply(partial.<R>fail(mapper)), failureHandler, charsetDecoder);
      }
    };
  }

  default <R> ReplyParser<R> fail(Throwable e) {
    return new ReplyParser<R>() {
      @Override
      public <U> U parseReply(ByteBuffer buffer, Function<? super R, U> resultHandler,
          PartialReplyHandler<? super R, U> partialReplyHandler, FailureHandler<U> failureHandler,
          CharsetDecoder charsetDecoder) {
        return ReplyParser.this.parseReply(buffer, value -> failureHandler.failure(e),
            partial -> partialReplyHandler.partialReply(partial.<R>fail(e)), e1 -> failureHandler.failure(e),
            charsetDecoder);
      }
    };
  }

  static <T1, T2, R> ReplyParser<R> combine(ReplyParser<? extends T1> parser1, ReplyParser<? extends T2> parser2,
      BiFunction<? super T1, ? super T2, ? extends R> fn) {
    return CombiningReplyParser.combine(parser1, parser2).mapToParser(fn);
  }

  interface PartialReplyHandler<T, U> extends Parser.PartialHandler<T, U> {
    U partialReply(ReplyParser<? extends T> partial);

    @Override
    default U partial(Parser<? extends T> partial) {
      return partialReply(partial);
    }
  }

  interface FailureHandler<U> {
    U failure(Throwable e);
  }
}

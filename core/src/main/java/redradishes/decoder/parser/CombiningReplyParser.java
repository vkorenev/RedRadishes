package redradishes.decoder.parser;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

interface CombiningReplyParser<T1, T2> {
  static <T1, T2> CombiningReplyParser<T1, T2> combine(ReplyParser<? extends T1> parser1,
      ReplyParser<? extends T2> parser2) {
    return new CombiningReplyParser<T1, T2>() {
      @Override
      public <U> U parseReply(ByteBuffer buffer, BiFunction<? super T1, ? super T2, ? extends U> resultHandler,
          PartialReplyHandler<? super T1, ? super T2, U> partialReplyHandler,
          ReplyParser.FailureHandler<U> failureHandler, CharsetDecoder charsetDecoder) {
        return parse1(parser1, buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
      }

      private <U> U parse1(ReplyParser<? extends T1> parser1, ByteBuffer buffer,
          BiFunction<? super T1, ? super T2, ? extends U> resultHandler,
          PartialReplyHandler<? super T1, ? super T2, U> partialReplyHandler,
          ReplyParser.FailureHandler<U> failureHandler, CharsetDecoder charsetDecoder) {
        return parser1.parseReply(buffer, new Function<T1, U>() {
          @Override
          public U apply(@Nullable T1 value1) {
            return parse2(buffer, resultHandler, partialReplyHandler, failureHandler, value1, parser2);
          }

          private <U1> U1 parse2(ByteBuffer buffer, BiFunction<? super T1, ? super T2, ? extends U1> resultHandler,
              PartialReplyHandler<? super T1, ? super T2, U1> partialReplyHandler,
              ReplyParser.FailureHandler<U1> failureHandler, @Nullable T1 value1, ReplyParser<? extends T2> parser2) {
            return parser2.parseReply(buffer, value2 -> resultHandler.apply(value1, value2),
                partial -> partialReplyHandler.partialReply(new CombiningReplyParser<T1, T2>() {
                  @Override
                  public <U2> U2 parseReply(ByteBuffer buffer,
                      BiFunction<? super T1, ? super T2, ? extends U2> resultHandler,
                      PartialReplyHandler<? super T1, ? super T2, U2> partialReplyHandler,
                      ReplyParser.FailureHandler<U2> failureHandler, CharsetDecoder charsetDecoder) {
                    return parse2(buffer, resultHandler, partialReplyHandler, failureHandler, value1, partial);
                  }
                }), failureHandler, charsetDecoder);
          }
        }, partial -> partialReplyHandler.partialReply(new CombiningReplyParser<T1, T2>() {
          @Override
          public <U1> U1 parseReply(ByteBuffer buffer, BiFunction<? super T1, ? super T2, ? extends U1> resultHandler,
              PartialReplyHandler<? super T1, ? super T2, U1> partialReplyHandler,
              ReplyParser.FailureHandler<U1> failureHandler, CharsetDecoder charsetDecoder) {
            return parse1(partial, buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
          }
        }), failureHandler, charsetDecoder);
      }
    };
  }

  <U> U parseReply(ByteBuffer buffer, BiFunction<? super T1, ? super T2, ? extends U> resultHandler,
      PartialReplyHandler<? super T1, ? super T2, U> partialReplyHandler, ReplyParser.FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder);

  default <R> ReplyParser<R> mapToParser(BiFunction<? super T1, ? super T2, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return new ReplyParser<R>() {
      @Override
      public <U> U parseReply(ByteBuffer buffer, Function<? super R, U> resultHandler,
          PartialReplyHandler<? super R, U> partialReplyHandler, FailureHandler<U> failureHandler,
          CharsetDecoder charsetDecoder) {
        return CombiningReplyParser.this
            .parseReply(buffer, (value1, value2) -> resultHandler.apply(mapper.apply(value1, value2)),
                partial -> partialReplyHandler.partialReply(partial.mapToParser(mapper)), failureHandler,
                charsetDecoder);
      }
    };
  }

  interface PartialReplyHandler<T1, T2, R> {
    R partialReply(CombiningReplyParser<? extends T1, ? extends T2> partial);
  }
}

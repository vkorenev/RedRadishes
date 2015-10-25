package redradishes.decoder.parser;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface SeqParser<T1, T2> {
  static <T1, T2> SeqParser<T1, T2> seq(Parser<T1> parser1, Parser<T2> parser2) {
    return new SeqParser<T1, T2>() {
      @Override
      public <R> R parse(ByteBuffer buffer, BiFunction<? super T1, ? super T2, R> resultHandler,
          PartialHandler<? super T1, ? super T2, R> partialHandler, CharsetDecoder charsetDecoder) {
        return doParse1(parser1, buffer, resultHandler, partialHandler, charsetDecoder);
      }

      private <R> R doParse1(Parser<? extends T1> parser1, ByteBuffer buffer,
          BiFunction<? super T1, ? super T2, R> resultHandler, PartialHandler<? super T1, ? super T2, R> partialHandler,
          CharsetDecoder charsetDecoder) {
        return parser1.parse(buffer, new Function<T1, R>() {
          @Override
          public R apply(@Nullable T1 value1) {
            return doParse2(parser2, buffer, resultHandler, partialHandler, value1, charsetDecoder);
          }

          private <R1> R1 doParse2(Parser<? extends T2> parser2, ByteBuffer buffer,
              BiFunction<? super T1, ? super T2, R1> resultHandler,
              PartialHandler<? super T1, ? super T2, R1> partialHandler, @Nullable T1 value1,
              CharsetDecoder charsetDecoder) {
            return parser2.parse(buffer, value2 -> resultHandler.apply(value1, value2),
                partial -> partialHandler.partial(new SeqParser<T1, T2>() {
                  @Override
                  public <R2> R2 parse(ByteBuffer buffer, BiFunction<? super T1, ? super T2, R2> resultHandler,
                      PartialHandler<? super T1, ? super T2, R2> partialHandler, CharsetDecoder charsetDecoder) {
                    return doParse2(partial, buffer, resultHandler, partialHandler, value1, charsetDecoder);
                  }
                }), charsetDecoder);
          }
        }, partial -> partialHandler.partial(new SeqParser<T1, T2>() {
          @Override
          public <R1> R1 parse(ByteBuffer buffer, BiFunction<? super T1, ? super T2, R1> resultHandler,
              PartialHandler<? super T1, ? super T2, R1> partialHandler, CharsetDecoder charsetDecoder) {
            return doParse1(partial, buffer, resultHandler, partialHandler, charsetDecoder);
          }
        }), charsetDecoder);
      }
    };
  }

  <R> R parse(ByteBuffer buffer, BiFunction<? super T1, ? super T2, R> resultHandler,
      PartialHandler<? super T1, ? super T2, R> partialHandler, CharsetDecoder charsetDecoder);

  default <R> Parser<R> mapToParser(BiFunction<? super T1, ? super T2, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return new Parser<R>() {
      @Override
      public <U> U parse(ByteBuffer buffer, Function<? super R, U> resultHandler,
          PartialHandler<? super R, U> partialHandler, CharsetDecoder charsetDecoder) {
        return SeqParser.this.parse(buffer, (value1, value2) -> resultHandler.apply(mapper.apply(value1, value2)),
            partial -> partialHandler.partial(partial.mapToParser(mapper)), charsetDecoder);
      }
    };
  }

  interface PartialHandler<T1, T2, R> {
    R partial(SeqParser<? extends T1, ? extends T2> partial);
  }
}

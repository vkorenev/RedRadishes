package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;
import java.util.function.LongFunction;

public abstract class LongParser {
  static final LongParser PARSER = new LongParser() {
    @Override
    <T> T parse(ByteBuffer buffer, LongFunction<T> resultHandler, PartialHandler<T> partialHandler) {
      return doParse(buffer, resultHandler, partialHandler, false, 0, SIGN_OR_DIGIT);
    }
  };
  public static final Parser<Integer> INTEGER_PARSER = PARSER.mapToParser(l -> (int) l);
  public static final Parser<Long> LONG_PARSER = PARSER.mapToParser(l -> l);
  private static final int SIGN_OR_DIGIT = 0;
  private static final int DIGIT = 1;
  private static final int WAITING_FOR_LF = 2;

  abstract <T> T parse(ByteBuffer buffer, LongFunction<T> resultHandler, PartialHandler<T> partialHandler);

  private static <T> T doParse(ByteBuffer buffer, LongFunction<T> resultHandler, PartialHandler<T> partialHandler,
      boolean negative, long num, int state) {
    while (buffer.hasRemaining()) {
      byte b = buffer.get();
      switch (state) {
        case SIGN_OR_DIGIT:
          state = DIGIT;
          if (b == '-') {
            negative = true;
            break;
          }
        case DIGIT:
          switch (b) {
            case '0':
              num *= 10;
              break;
            case '1':
              num = num * 10 + 1;
              break;
            case '2':
              num = num * 10 + 2;
              break;
            case '3':
              num = num * 10 + 3;
              break;
            case '4':
              num = num * 10 + 4;
              break;
            case '5':
              num = num * 10 + 5;
              break;
            case '6':
              num = num * 10 + 6;
              break;
            case '7':
              num = num * 10 + 7;
              break;
            case '8':
              num = num * 10 + 8;
              break;
            case '9':
              num = num * 10 + 9;
              break;
            case '\r':
              state = WAITING_FOR_LF;
              break;
            default:
              throw new IllegalStateException("Unexpected character: " + (char) b);
          }
          break;
        case WAITING_FOR_LF:
          if (b == '\n') {
            return resultHandler.apply(negative ? -num : num);
          } else {
            throw new IllegalStateException("LF is expected");
          }
      }
    }
    return partialHandler.partial(new LongPartial(negative, num, state));
  }

  <T> Parser<T> mapToParser(LongFunction<T> longFunction) {
    return new ParserAdaptor<>(this, longFunction);
  }

  interface PartialHandler<T> {
    T partial(LongParser partial);
  }

  private static class LongPartial extends LongParser {
    private final boolean negative;
    private final long num;
    private final int state;

    private LongPartial(boolean negative, long num, int state) {
      this.negative = negative;
      this.num = num;
      this.state = state;
    }

    @Override
    <T> T parse(ByteBuffer buffer, LongFunction<T> resultHandler, PartialHandler<T> partialHandler) {
      return doParse(buffer, resultHandler, partialHandler, negative, num, state);
    }
  }

  private static class ParserAdaptor<T> implements Parser<T> {
    private final LongParser parser;
    private final LongFunction<T> longFunction;

    private ParserAdaptor(LongParser parser, LongFunction<T> longFunction) {
      this.parser = parser;
      this.longFunction = longFunction;
    }

    @Override
    public <U> U parse(ByteBuffer buffer, Function<? super T, U> resultHandler,
        PartialHandler<? super T, U> partialHandler, CharsetDecoder charsetDecoder) {
      return parser.parse(buffer, value -> resultHandler.apply(longFunction.apply(value)),
          partial -> partialHandler.partial(new ParserAdaptor<>(partial, longFunction)));
    }
  }
}

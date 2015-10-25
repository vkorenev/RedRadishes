package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

public class StringParser implements Parser<CharSequence> {
  public static final Parser<CharSequence> STRING_PARSER = new StringParser();
  private static final int READING = 0;
  private static final int WAITING_FOR_LF = 1;

  private StringParser() {
  }

  @Override
  public <U> U parse(ByteBuffer buffer, Function<? super CharSequence, U> resultHandler,
      PartialHandler<? super CharSequence, U> partialHandler, CharsetDecoder charsetDecoder) {
    return doParse(buffer, resultHandler, partialHandler, new StringBuilder(), READING);
  }

  private static <U> U doParse(ByteBuffer buffer, Function<? super CharSequence, U> resultHandler,
      PartialHandler<? super CharSequence, U> partialHandler, StringBuilder stringBuilder, int state) {
    while (buffer.hasRemaining()) {
      byte b = buffer.get();
      switch (state) {
        case READING:
          if (b == '\r') {
            state = WAITING_FOR_LF;
          } else {
            stringBuilder.append((char) b);
          }
          break;
        case WAITING_FOR_LF:
          if (b == '\n') {
            return resultHandler.apply(stringBuilder);
          } else {
            throw new IllegalStateException("LF is expected");
          }
      }
    }
    int state1 = state;
    return partialHandler.partial(new Parser<CharSequence>() {
      @Override
      public <U1> U1 parse(ByteBuffer buffer, Function<? super CharSequence, U1> resultHandler,
          PartialHandler<? super CharSequence, U1> partialHandler, CharsetDecoder charsetDecoder) {
        return doParse(buffer, resultHandler, partialHandler, stringBuilder, state1);
      }
    });
  }
}

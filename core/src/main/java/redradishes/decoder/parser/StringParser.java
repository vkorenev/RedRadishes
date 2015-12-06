package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;
import java.util.function.Supplier;

public class StringParser implements Parser<CharSequence> {
  private static final int READING = 0;
  private static final int WAITING_FOR_LF = 1;
  public static final Parser<CharSequence> STRING_PARSER = new StringParser(StringBuilder::new, READING);
  private final Supplier<StringBuilder> stringBuilderSupplier;
  private final int state0;

  private StringParser(Supplier<StringBuilder> stringBuilderSupplier, int state0) {
    this.stringBuilderSupplier = stringBuilderSupplier;
    this.state0 = state0;
  }

  @Override
  public <U> U parse(ByteBuffer buffer, Function<? super CharSequence, U> resultHandler,
      PartialHandler<? super CharSequence, U> partialHandler, CharsetDecoder charsetDecoder) {
    StringBuilder stringBuilder = stringBuilderSupplier.get();
    int state = state0;
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
    return partialHandler.partial(new StringParser(() -> stringBuilder, state));
  }
}

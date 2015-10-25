package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

public class PrefixedParser<T> implements Parser<T> {
  private final char marker;
  private final Parser<T> parser;

  public PrefixedParser(char marker, Parser<T> parser) {
    this.marker = marker;
    this.parser = parser;
  }

  @Override
  public <U> U parse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialHandler<? super T, U> partialHandler, CharsetDecoder charsetDecoder) {
    if (buffer.hasRemaining()) {
      byte b = buffer.get();
      if (b == marker) {
        return parser.parse(buffer, resultHandler, partialHandler, charsetDecoder);
      } else {
        throw new IllegalStateException('\'' + marker + "' is expected but '" + (char) b + "' was found");
      }
    } else {
      return partialHandler.partial(this);
    }
  }
}

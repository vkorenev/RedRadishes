package redradishes.decoder.parser;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

public class ExpectedResultParser<T> implements Parser<T> {
  private final byte[] expectedBytes;
  private final int pos0;
  private final T reply;

  public ExpectedResultParser(byte[] expectedBytes, @Nullable T reply) {
    this(expectedBytes, 0, reply);
  }

  private ExpectedResultParser(byte[] expectedBytes, int pos0, @Nullable T reply) {
    this.expectedBytes = expectedBytes;
    this.pos0 = pos0;
    this.reply = reply;
  }

  @Override
  public <U> U parse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialHandler<? super T, U> partialHandler, CharsetDecoder charsetDecoder) {
    int pos = pos0;
    while (pos < expectedBytes.length) {
      if (buffer.hasRemaining()) {
        byte actual = buffer.get();
        byte expected = expectedBytes[pos];
        if (actual != expected) {
          throw new IllegalStateException('\'' + expected + "' is expected but '" + (char) actual + "' was found");
        }
        pos++;
      } else {
        return partialHandler.partial(new ExpectedResultParser<>(expectedBytes, pos, reply));
      }
    }
    return resultHandler.apply(reply);
  }
}

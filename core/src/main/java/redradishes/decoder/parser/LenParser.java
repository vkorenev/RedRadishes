package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;
import java.util.function.IntFunction;

public class LenParser<T> implements Parser<T> {
  private final IntFunction<Parser<T>> bodyParserFactory;
  private final LongParser lengthParser;

  public LenParser(IntFunction<Parser<T>> bodyParserFactory) {
    this(bodyParserFactory, LongParser.PARSER);
  }

  private LenParser(IntFunction<Parser<T>> bodyParserFactory, LongParser lengthParser) {
    this.bodyParserFactory = bodyParserFactory;
    this.lengthParser = lengthParser;
  }

  @Override
  public <U> U parse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialHandler<? super T, U> partialHandler, CharsetDecoder charsetDecoder) {
    return lengthParser.parse(buffer, value -> {
      if (value == -1) {
        return resultHandler.apply(null);
      } else {
        return bodyParserFactory.apply((int) value).parse(buffer, resultHandler, partialHandler, charsetDecoder);
      }
    }, partial -> partialHandler.partial(new LenParser<>(bodyParserFactory, partial)));
  }
}

package redradishes.decoder.parser;

import redradishes.decoder.ArrayBuilderFactory;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

public class ArrayParser<T, E> implements Parser<T> {
  private final int len;
  private final ArrayBuilderFactory<E, ? extends T> builderFactory;
  private final Parser<E> elementParser;

  public ArrayParser(int len, ArrayBuilderFactory<E, ? extends T> builderFactory, Parser<E> elementParser) {
    this.len = len;
    this.builderFactory = builderFactory;
    this.elementParser = elementParser;
  }

  @Override
  public <U> U parse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialHandler<? super T, U> partialHandler, CharsetDecoder charsetDecoder) {
    return doParse(buffer, resultHandler, partialHandler, builderFactory.create(len), len, elementParser,
        charsetDecoder);
  }

  private <U> U doParse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialHandler<? super T, U> partialHandler, ArrayBuilderFactory.Builder<E, ? extends T> builder, int remaining,
      Parser<? extends E> elemParser, CharsetDecoder charsetDecoder) {
    while (remaining > 0) {
      Parser<T> partial = parsePartial(buffer, builder, remaining, elemParser, charsetDecoder);
      if (partial != null) {
        return partialHandler.partial(partial);
      } else {
        remaining--;
        elemParser = elementParser;
      }
    }
    return resultHandler.apply(builder.build());
  }

  private Parser<T> parsePartial(ByteBuffer buffer, ArrayBuilderFactory.Builder<E, ? extends T> builder, int remaining,
      Parser<? extends E> elemParser, CharsetDecoder charsetDecoder) {
    return elemParser.parse(buffer, value -> {
      builder.add(value);
      return null;
    }, partial -> new Parser<T>() {
      @Override
      public <U1> U1 parse(ByteBuffer buffer, Function<? super T, U1> resultHandler,
          PartialHandler<? super T, U1> partialHandler, CharsetDecoder charsetDecoder) {
        return doParse(buffer, resultHandler, partialHandler, builder, remaining, partial, charsetDecoder);
      }
    }, charsetDecoder);
  }
}

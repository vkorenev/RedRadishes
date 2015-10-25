package redradishes.decoder.parser;

import redradishes.decoder.MapBuilderFactory;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

public class ArrayAsMapParser<T, K, V> implements Parser<T> {
  private final int len;
  private final MapBuilderFactory<K, V, ? extends T> builderFactory;
  private final SeqParser<K, V> kvParser;

  public ArrayAsMapParser(int len, MapBuilderFactory<K, V, ? extends T> builderFactory, SeqParser<K, V> kvParser) {
    this.len = len;
    this.builderFactory = builderFactory;
    this.kvParser = kvParser;
  }

  @Override
  public <U> U parse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialHandler<? super T, U> partialHandler, CharsetDecoder charsetDecoder) {
    return doParse(buffer, resultHandler, partialHandler, builderFactory.create(len), len, kvParser, charsetDecoder);
  }

  private <U> U doParse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialHandler<? super T, U> partialHandler, MapBuilderFactory.Builder<K, V, ? extends T> builder, int remaining,
      SeqParser<? extends K, ? extends V> kvSeqParser, CharsetDecoder charsetDecoder) {
    while (remaining > 0) {
      Parser<T> partial = parsePartial(buffer, builder, remaining, kvSeqParser, charsetDecoder);
      if (partial != null) {
        return partialHandler.partial(partial);
      } else {
        remaining--;
        kvSeqParser = kvParser;
      }
    }
    return resultHandler.apply(builder.build());
  }

  private Parser<T> parsePartial(ByteBuffer buffer, MapBuilderFactory.Builder<K, V, ? extends T> builder, int remaining,
      SeqParser<? extends K, ? extends V> kvSeqParser, CharsetDecoder charsetDecoder) {
    return kvSeqParser.parse(buffer, (key, value) -> {
      builder.put(key, value);
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

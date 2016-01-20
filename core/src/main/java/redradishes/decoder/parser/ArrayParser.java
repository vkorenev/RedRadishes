package redradishes.decoder.parser;

import com.google.common.base.Throwables;
import redradishes.decoder.ArrayBuilderFactory;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

public class ArrayParser<T, E> implements ReplyParser<T> {
  private final int len;
  private final ArrayBuilderFactory<E, ? extends T> builderFactory;
  private final ReplyParser<E> elementParser;

  public ArrayParser(int len, ArrayBuilderFactory<E, ? extends T> builderFactory, ReplyParser<E> elementParser) {
    this.len = len;
    this.builderFactory = builderFactory;
    this.elementParser = elementParser;
  }

  @Override
  public <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    return doParse(buffer, resultHandler, partialReplyHandler, builderFactory.create(len), len, elementParser,
        charsetDecoder);
  }

  private <U> U doParse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, ArrayBuilderFactory.Builder<E, ? extends T> builder,
      int remaining, ReplyParser<? extends E> elemParser, CharsetDecoder charsetDecoder) {
    while (remaining > 0) {
      ReplyParser<T> partial = parsePartial(buffer, builder, remaining, elemParser, charsetDecoder);
      if (partial != null) {
        return partialReplyHandler.partialReply(partial);
      } else {
        remaining--;
        elemParser = elementParser;
      }
    }
    return resultHandler.apply(builder.build());
  }

  private ReplyParser<T> parsePartial(ByteBuffer buffer, ArrayBuilderFactory.Builder<E, ? extends T> builder,
      int remaining, ReplyParser<? extends E> elemParser, CharsetDecoder charsetDecoder) {
    return elemParser.parseReply(buffer, value -> {
      builder.add(value);
      return null;
    }, partial -> new ReplyParser<T>() {
      @Override
      public <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
          PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
          CharsetDecoder charsetDecoder) {
        return doParse(buffer, resultHandler, partialReplyHandler, builder, remaining, partial, charsetDecoder);
      }
    }, e -> {
      throw Throwables.propagate(e);
    }, charsetDecoder);
  }
}

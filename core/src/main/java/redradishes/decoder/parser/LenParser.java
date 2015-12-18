package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;
import java.util.function.IntFunction;

class LenParser<T> implements ReplyParser<T> {
  private final IntFunction<? extends ReplyParser<T>> bodyParserFactory;
  private final LongParser lengthParser;

  LenParser(IntFunction<? extends ReplyParser<T>> bodyParserFactory) {
    this(bodyParserFactory, LongParser.PARSER);
  }

  private LenParser(IntFunction<? extends ReplyParser<T>> bodyParserFactory, LongParser lengthParser) {
    this.bodyParserFactory = bodyParserFactory;
    this.lengthParser = lengthParser;
  }

  @Override
  public <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    return lengthParser.parse(buffer, value -> {
      if (value == -1) {
        return resultHandler.apply(null);
      } else {
        return bodyParserFactory.apply((int) value)
            .parseReply(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
      }
    }, partial -> partialReplyHandler.partialReply(new LenParser<>(bodyParserFactory, partial)));
  }
}

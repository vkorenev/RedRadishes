package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

class AnyReplyParser<T> implements ReplyParser<T> {
  private final ReplyParser<? extends T> simpleStrParser;
  private final ReplyParser<? extends T> errorParser;
  private final ReplyParser<? extends T> integerParser;
  private final ReplyParser<? extends T> bulkStrParser;
  private final ReplyParser<? extends T> arrayParser;

  AnyReplyParser(ReplyParser<? extends T> simpleStrParser, ReplyParser<? extends T> errorParser,
      ReplyParser<? extends T> integerParser, ReplyParser<? extends T> bulkStrParser,
      ReplyParser<? extends T> arrayParser) {
    this.simpleStrParser = simpleStrParser;
    this.errorParser = errorParser;
    this.integerParser = integerParser;
    this.bulkStrParser = bulkStrParser;
    this.arrayParser = arrayParser;
  }

  @Override
  public <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    if (buffer.hasRemaining()) {
      byte b = buffer.get();
      switch (b) {
        case '+':
          return simpleStrParser.parseReply(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
        case '-':
          return errorParser.parseReply(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
        case ':':
          return integerParser.parseReply(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
        case '$':
          return bulkStrParser.parseReply(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
        case '*':
          return arrayParser.parseReply(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
        default:
          throw new IllegalStateException("Marker is expected but '" + (char) b + "' was found");
      }
    } else {
      return partialReplyHandler.partialReply(this);
    }
  }
}

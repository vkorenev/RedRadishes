package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

public class ErrorParser<T> implements ReplyParser<T> {

  private final Parser<? extends CharSequence> errorParser;

  private ErrorParser(Parser<? extends CharSequence> errorParser) {
    this.errorParser = errorParser;
  }

  public ErrorParser() {
    this(StringParser.STRING_PARSER);
  }

  @Override
  public <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    return doParse(buffer, partialReplyHandler, failureHandler, errorParser, charsetDecoder);
  }

  private <U> U doParse(ByteBuffer buffer, PartialReplyHandler<? super T, U> partialReplyHandler,
      FailureHandler<U> failureHandler, Parser<? extends CharSequence> errorParser, CharsetDecoder charsetDecoder) {
    return errorParser.parse(buffer, failureHandler::failure,
        partial -> partialReplyHandler.partialReply(new ErrorParser<T>(partial)), charsetDecoder);
  }
}

package redradishes.decoder.parser;

import redradishes.RedisException;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

import static redradishes.decoder.parser.CharAppendingParser.CHAR_SEQUENCE_PARSER;

public class ErrorParser<T> implements ReplyParser<T> {

  private final ReplyParser<? extends CharSequence> errorParser;

  private ErrorParser(ReplyParser<? extends CharSequence> errorParser) {
    this.errorParser = errorParser;
  }

  public ErrorParser() {
    this(CHAR_SEQUENCE_PARSER);
  }

  @Override
  public <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    return errorParser.parseReply(buffer, message -> failureHandler.failure(new RedisException(message.toString())),
        partial -> partialReplyHandler.partialReply(new ErrorParser<T>(partial)), failureHandler, charsetDecoder);
  }
}

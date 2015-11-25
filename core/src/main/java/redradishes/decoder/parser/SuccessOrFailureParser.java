package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

class SuccessOrFailureParser<T> implements ReplyParser<T> {
  private final ErrorParser<T> errorParser = new ErrorParser<>();
  private final ExpectedResultParser<T> nilParser = new ExpectedResultParser<>(new byte[]{'-', '1', '\r', '\n'}, null);
  private final char marker;
  private final Parser<T> parser;

  SuccessOrFailureParser(char marker, Parser<T> parser) {
    this.marker = marker;
    this.parser = parser;
  }

  @Override
  public <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    if (buffer.hasRemaining()) {
      byte b = buffer.get();
      if (b == marker) {
        return parser.parse(buffer, resultHandler, partialReplyHandler, charsetDecoder);
      } else {
        switch (b) {
          case '-':
            return errorParser.parseReply(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
          case '$':
            return nilParser.parseReply(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
          default:
            throw new IllegalStateException("'" + marker + "' is expected but '" + (char) b + "' was found");
        }
      }
    } else {
      return partialReplyHandler.partialReply(this);
    }
  }
}

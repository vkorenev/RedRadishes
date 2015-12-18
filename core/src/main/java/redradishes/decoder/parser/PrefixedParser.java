package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

class PrefixedParser<T> implements ReplyParser<T> {
  private final char marker;
  private final ReplyParser<T> parser;

  PrefixedParser(char marker, ReplyParser<T> parser) {
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
        return parser.parseReply(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
      } else {
        throw new IllegalStateException('\'' + marker + "' is expected but '" + (char) b + "' was found");
      }
    } else {
      return partialReplyHandler.partialReply(this);
    }
  }
}

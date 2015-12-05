package redradishes.decoder.parser;

import redradishes.decoder.ReplyParseException;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

class UnexpectedSimpleReplyParser<T> implements ReplyParser<T> {
  private static final int READING = 0;
  private static final int WAITING_FOR_LF = 1;
  private final String message;
  private final int state0;

  UnexpectedSimpleReplyParser(String replyType) {
    this("Unexpected " + replyType + " reply", READING);
  }

  private UnexpectedSimpleReplyParser(String message, int state0) {
    this.message = message;
    this.state0 = state0;
  }

  @Override
  public <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    int state = state0;
    while (buffer.hasRemaining()) {
      byte b = buffer.get();
      switch (state) {
        case READING:
          if (b == '\r') {
            state = WAITING_FOR_LF;
          }
          break;
        case WAITING_FOR_LF:
          if (b == '\n') {
            return failureHandler.failure(new ReplyParseException(message));
          } else {
            throw new IllegalStateException("LF is expected");
          }
      }
    }
    return partialReplyHandler.partialReply(new UnexpectedSimpleReplyParser<T>(message, state));
  }
}

package redradishes.decoder.parser;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;
import java.util.function.Supplier;

public class CharAppendingParser<A extends Appendable> implements ReplyParser<A> {
  public static final ReplyParser<? extends CharSequence> CHAR_SEQUENCE_PARSER =
      new CharAppendingParser<>(StringBuilder::new);
  private static final int READING = 0;
  private static final int WAITING_FOR_LF = 1;
  private final Supplier<A> appendableSupplier;
  private final Exception exception0;
  private final int state0;

  CharAppendingParser(Supplier<A> appendableSupplier) {
    this(appendableSupplier, null, READING);
  }

  private CharAppendingParser(Supplier<A> appendableSupplier, @Nullable Exception exception0, int state0) {
    this.appendableSupplier = appendableSupplier;
    this.exception0 = exception0;
    this.state0 = state0;
  }

  @Override
  public <U> U parseReply(ByteBuffer buffer, Function<? super A, U> resultHandler,
      PartialReplyHandler<? super A, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    A appendable = appendableSupplier.get();
    int state = state0;
    Exception exception = exception0;
    while (buffer.hasRemaining()) {
      byte b = buffer.get();
      switch (state) {
        case READING:
          if (b == '\r') {
            state = WAITING_FOR_LF;
          } else {
            try {
              appendable.append((char) b);
            } catch (Exception e) {
              if (exception == null) {
                exception = e;
              }
            }
          }
          break;
        case WAITING_FOR_LF:
          if (b == '\n') {
            return exception == null ? resultHandler.apply(appendable) : failureHandler.failure(exception);
          } else {
            throw new IllegalStateException("LF is expected");
          }
      }
    }
    return partialReplyHandler.partialReply(new CharAppendingParser<>(() -> appendable, exception, state));
  }
}

package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.lang.Math.min;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public abstract class TestParser<R> implements ReplyParser<R> {
  private final int replyLength;
  private final int chunkLength;

  protected TestParser(int replyLength, int chunkLength) {
    this.replyLength = replyLength;
    this.chunkLength = chunkLength;
  }

  @Override
  public <U> U parseReply(ByteBuffer buffer, Function<? super R, U> resultHandler,
      PartialReplyHandler<? super R, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    int remaining = buffer.remaining();
    if (remaining >= replyLength) {
      readAndCheck(buffer, replyLength);
      return getResult(resultHandler, failureHandler);
    } else {
      int len = min(chunkLength, remaining);
      readAndCheck(buffer, len);
      return partialReplyHandler.partialReply(getPartial(replyLength - len));
    }
  }

  private void readAndCheck(ByteBuffer buffer, int len) {
    byte[] actual = new byte[len];
    buffer.get(actual);
    byte[] expected = new byte[len];
    for (int i = 0; i < len; i++) {
      expected[i] = (byte) (replyLength - i);
    }
    assertThat(actual, equalTo(expected));
  }

  protected abstract ReplyParser<R> getPartial(int remainingLen);

  protected abstract <U> U getResult(Function<? super R, U> resultHandler, FailureHandler<U> failureHandler);

  public static <R> ReplyParser<R> succeedingParser(R result, int replyLength) {
    return succeedingParser(result, replyLength, replyLength);
  }

  public static <R> ReplyParser<R> succeedingParser(R result, int replyLength, int chunkLength) {
    return new TestParser<R>(replyLength, chunkLength) {
      @Override
      public <U> U getResult(Function<? super R, U> resultHandler, FailureHandler<U> failureHandler) {
        return resultHandler.apply(result);
      }

      @Override
      public ReplyParser<R> getPartial(int remainingLength) {
        return succeedingParser(result, remainingLength, chunkLength);
      }
    };
  }

  public static <R> ReplyParser<R> failingParser(Exception exception, int replyLength) {
    return failingParser(exception, replyLength, replyLength);
  }

  public static <R> ReplyParser<R> failingParser(Exception exception, int replyLength, int chunkLength) {
    return new TestParser<R>(replyLength, chunkLength) {
      @Override
      public <U> U getResult(Function<? super R, U> resultHandler, FailureHandler<U> failureHandler) {
        return failureHandler.failure(exception);
      }

      @Override
      public ReplyParser<R> getPartial(int remainingLength) {
        return failingParser(exception, remainingLength, chunkLength);
      }
    };
  }

  public static byte[] parsableInput(int... lengths) {
    byte[] result = new byte[IntStream.of(lengths).sum()];
    int i = 0;
    for (int length : lengths) {
      for (int j = length; j > 0; j--) {
        result[i++] = (byte) j;
      }
    }
    return result;
  }
}

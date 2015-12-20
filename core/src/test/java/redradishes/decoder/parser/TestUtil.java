package redradishes.decoder.parser;

import com.google.common.io.ByteArrayDataOutput;
import redradishes.decoder.parser.ReplyParser.FailureHandler;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class TestUtil {
  public static <T, U> U parseReply(ByteBuffer byteBuffer, int chunkSize, ReplyParser<T> parser,
      Function<? super T, U> resultHandler, FailureHandler<U> failureHandler, CharsetDecoder charsetDecoder) {
    ByteBuffer src;
    if (byteBuffer.remaining() > chunkSize) {
      src = byteBuffer.slice();
      src.limit(src.position() + chunkSize);
    } else {
      src = byteBuffer;
    }
    U result = parser.parseReply(src, resultHandler, partial -> {
      assertThat(src, not(sameInstance(byteBuffer)));
      byteBuffer.position(byteBuffer.position() + chunkSize - src.remaining());
      return parseReply(byteBuffer, chunkSize, partial, resultHandler, failureHandler, charsetDecoder);
    }, failureHandler, charsetDecoder);
    assertFalse("Remaining bytes: " + byteBuffer.remaining(), byteBuffer.hasRemaining());
    return result;
  }

  public static <T, R> Function<T, R> assertNoResult() {
    return result -> {
      throw new AssertionError("Unexpected result: " + result);
    };
  }

  public static <T> FailureHandler<T> assertNoFailure() {
    return e -> {
      throw new AssertionError("Unexpected failure", e);
    };
  }

  public static byte[] getByteString(byte[] bytes) {
    byte[] header = ('$' + Integer.toString(bytes.length) + "\r\n").getBytes(US_ASCII);
    byte[] target = Arrays.copyOf(header, header.length + bytes.length + 2);
    System.arraycopy(bytes, 0, target, header.length, bytes.length);
    target[target.length - 2] = '\r';
    target[target.length - 1] = '\n';
    return target;
  }

  public static void writeByteString(byte[] bytes, ByteArrayDataOutput out) {
    writeLenPrefix('$', bytes.length, out);
    out.write(bytes);
    out.write('\r');
    out.write('\n');
  }

  public static void writeLenPrefix(char marker, int length, ByteArrayDataOutput out) {
    out.write(marker);
    out.write(Integer.toString(length).getBytes(US_ASCII));
    out.write('\r');
    out.write('\n');
  }
}

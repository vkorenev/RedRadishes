package redradishes.decoder.parser;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
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
import static org.junit.Assume.assumeTrue;

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
    class Ref {
      ReplyParser<? extends T> partial;
    }
    Ref ref = new Ref();
    U result = parser.parseReply(src, resultHandler, partial -> {
      ref.partial = partial;
      return null;
    }, failureHandler, charsetDecoder);
    if (ref.partial != null) {
      assertThat(src, not(sameInstance(byteBuffer)));
      byteBuffer.position(byteBuffer.position() + chunkSize - src.remaining());
      return parseReply(byteBuffer, chunkSize, ref.partial, resultHandler, failureHandler, charsetDecoder);
    }
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

  public static byte[] encodeSimpleString(String value) {
    assumeTrue(value.indexOf('\r') == -1 && value.indexOf('\n') == -1);
    return ("+" + value + "\r\n").getBytes(US_ASCII);
  }

  public static byte[] encodeError(String value) {
    assumeTrue(value.indexOf('\r') == -1 && value.indexOf('\n') == -1);
    return ("-" + value + "\r\n").getBytes(US_ASCII);
  }

  public static byte[] encodeInteger(long num) {
    return (":" + num + "\r\n").getBytes(US_ASCII);
  }

  public static byte[] encodeByteString(byte[] bytes) {
    byte[] header = ('$' + Integer.toString(bytes.length) + "\r\n").getBytes(US_ASCII);
    byte[] target = Arrays.copyOf(header, header.length + bytes.length + 2);
    System.arraycopy(bytes, 0, target, header.length, bytes.length);
    target[target.length - 2] = '\r';
    target[target.length - 1] = '\n';
    return target;
  }

  public static byte[] encodeNilBulkString() {
    return "$-1\r\n".getBytes(US_ASCII);
  }

  public static byte[] encodeArray(byte[][] elements) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    writeArray(elements, out);
    return out.toByteArray();
  }

  public static byte[] encodeNilArray() {
    return "*-1\r\n".getBytes(US_ASCII);
  }

  public static byte[] encodeScanReply(long cursor, byte[][] elements) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    writeLenPrefix('*', 2, out);
    writeByteString(Long.toString(cursor).getBytes(US_ASCII), out);
    writeArray(elements, out);
    return out.toByteArray();
  }

  private static void writeArray(byte[][] elements, ByteArrayDataOutput out) {
    writeLenPrefix('*', elements.length, out);
    for (byte[] bytes : elements) {
      writeByteString(bytes, out);
    }
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

package redradishes.decoder.parser;

import com.google.common.io.ByteArrayDataOutput;
import redradishes.decoder.parser.ReplyParser.FailureHandler;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class TestUtil {
  public static Iterator<ByteBuffer> split(byte[] bytes, int chunkSize) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    int chunkNum = bytes.length / chunkSize + (bytes.length % chunkSize > 0 ? 1 : 0);
    return IntStream.iterate(0, i -> i + chunkSize).limit(chunkNum).mapToObj(start -> {
      ByteBuffer slice = byteBuffer.slice();
      slice.position(start).limit(min(start + chunkSize, slice.capacity()));
      return slice;
    }).iterator();
  }

  static <T, U> U parse(Iterator<ByteBuffer> chunks, Parser<T> parser, Function<? super T, U> resultHandler,
      CharsetDecoder charsetDecoder) {
    return parser.parse(chunks.next(), resultHandler, partial -> parse(chunks, partial, resultHandler, charsetDecoder),
        charsetDecoder);
  }

  public static <T, U> U parseReply(Iterator<ByteBuffer> chunks, ReplyParser<T> parser,
      Function<? super T, U> resultHandler, FailureHandler<U> failureHandler, CharsetDecoder charsetDecoder) {
    return parser.parseReply(chunks.next(), resultHandler,
        partial -> parseReply(chunks, partial, resultHandler, failureHandler, charsetDecoder), failureHandler,
        charsetDecoder);
  }

  public static <T> FailureHandler<T> throwingFailureHandler() {
    return message -> {
      throw new RuntimeException(message.toString());
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

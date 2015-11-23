package redradishes.decoder.parser;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.lang.Math.min;

class TestUtil {
  static Iterator<ByteBuffer> split(byte[] bytes, int chunkSize) {
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
}

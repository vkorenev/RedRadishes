package redradishes.encoder;

import com.google.common.base.Utf8;
import redradishes.UncheckedCharacterCodingException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static redradishes.encoder.ConstExpr.byteConst;
import static redradishes.encoder.ConstExpr.bytesConst;
import static redradishes.encoder.ConstExpr.newArg;
import static redradishes.encoder.ConstExpr.strConst;
import static redradishes.encoder.Encoder.bytesEnc;
import static redradishes.encoder.IntEncoder.byteEnc;

class RespEncoders {
  private static final byte[][] NUM_BYTES =
      IntStream.rangeClosed(10, 99).mapToObj(i -> Integer.toString(i).getBytes(US_ASCII)).toArray(byte[][]::new);
  private static final byte[] MIN_LONG_BYTES = "-9223372036854775808".getBytes(US_ASCII);
  private static final long[] SIZE_TABLE = LongStream.iterate(10, x -> x * 10).limit(18).map(x -> x - 1).toArray();
  private static final ConstExpr CR_LF = bytesConst(new byte[]{'\r', '\n'});
  private static final ConstExpr EMPTY_BULK_STRING = newArg().append(charConst('0')).append(CR_LF).append(CR_LF);

  public static IntEncoder array() {
    return charConst('*').append(intEnc()).append(CR_LF);
  }

  private static ConstExpr charConst(char c) {
    return byteConst((byte) c);
  }

  public static Encoder<CharSequence> strBulkString(CharsetEncoder charsetEncoder) {
    if (UTF_8.equals(charsetEncoder.charset())) {
      return Encoder.flatMap(s -> {
        if (s.length() == 0) {
          return EMPTY_BULK_STRING.constEnc();
        } else {
          return newArg().append(intEnc().map(Utf8::encodedLength)).append(CR_LF).append(strConst(s, charsetEncoder))
              .append(CR_LF);
        }
      });
    } else if (charsetEncoder.maxBytesPerChar() == 1.0) {
      return Encoder.flatMap(s -> {
        if (s.length() == 0) {
          return EMPTY_BULK_STRING.constEnc();
        } else {
          return newArg().append(intEnc().map(CharSequence::length)).append(CR_LF).append(strConst(s, charsetEncoder))
              .append(CR_LF);
        }
      });
    } else {
      return Encoder.flatMap(s -> {
        if (s.length() == 0) {
          return EMPTY_BULK_STRING.constEnc();
        } else {
          try {
            ByteBuffer byteBuffer = encode(s, charsetEncoder);
            int encodedLength = byteBuffer.remaining();
            return newArg().append(intEnc().encode(encodedLength).<CharSequence>constEnc()).append(CR_LF)
                .append(bytesConst(byteBuffer.array(), 0, encodedLength)).append(CR_LF);
          } catch (CharacterCodingException e) {
            throw new UncheckedCharacterCodingException(e);
          }
        }
      });
    }
  }

  private static ByteBuffer encode(CharSequence s, CharsetEncoder charsetEncoder) throws CharacterCodingException {
    int maxLength = (int) (s.length() * (double) charsetEncoder.maxBytesPerChar());
    ByteBuffer byteBuffer = ByteBuffer.allocate(maxLength);
    CharBuffer charBuffer = CharBuffer.wrap(s);
    CoderResult coderResult = charsetEncoder.reset().encode(charBuffer, byteBuffer, true);
    if (coderResult.isUnderflow()) {
      coderResult = charsetEncoder.flush(byteBuffer);
    }
    if (!coderResult.isUnderflow()) {
      coderResult.throwException();
    }
    byteBuffer.flip();
    return byteBuffer;
  }

  public static Encoder<byte[]> bytesBulkString() {
    return newArg().append(arrayLenEnc()).append(CR_LF).zip(bytesEnc()).append(CR_LF);
  }

  private static Encoder<byte[]> arrayLenEnc() {
    return intEnc().map(arr -> arr.length);
  }

  public static Encoder<Integer> intBulkString() {
    return Encoder.flatMap(num -> {
      if (num >= 0 && num <= 9) {
        return oneDigitAsBulkString();
      } else if (num >= 10 && num <= 99) {
        return twoDigitsAsBulkString();
      } else {
        return longAsBulkString().map(Integer::longValue);
      }
    });
  }

  public static Encoder<Long> longBulkString() {
    return Encoder.flatMap(num -> {
      if (num >= 0 && num <= 9) {
        return oneDigitAsBulkString().map(Long::intValue);
      } else if (num >= 10 && num <= 99) {
        return twoDigitsAsBulkString().map(Long::intValue);
      } else if (num == Long.MIN_VALUE) {
        return minLongAsBulkString().constEnc();
      } else {
        return longAsBulkString();
      }
    });
  }

  private static Encoder<Integer> oneDigitAsBulkString() {
    return newArg().append(charConst('1')).append(CR_LF).append(byteEnc().map((Integer num) -> (byte) ('0' + num)))
        .append(CR_LF);
  }

  private static Encoder<Integer> twoDigitsAsBulkString() {
    return newArg().append(charConst('2')).append(CR_LF).append(bytesEnc().map((Integer num) -> NUM_BYTES[num - 10]))
        .append(CR_LF);
  }

  private static ConstExpr minLongAsBulkString() {
    return newArg().append(charConst('2')).append(charConst('0')).append(CR_LF).append(bytesConst(MIN_LONG_BYTES))
        .append(CR_LF);
  }

  private static Encoder<Long> longAsBulkString() {
    return newArg().append(Encoder.flatMap((Long num) -> {
      byte[] bytes = toBytes(num);
      int len = bytes.length;
      return (len <= 9 ? byteConst((byte) ('0' + len)) : bytesConst(NUM_BYTES[len - 10])).append(CR_LF)
          .append(bytesConst(bytes)).constEnc();
    })).append(CR_LF);
  }

  private static IntEncoder intEnc() {
    return IntEncoder.flatMap(num -> {
      if (num >= 0 && num <= 9) {
        return byteEnc().mapToIntEncoder(i -> '0' + i);
      } else if (num >= 10 && num <= 99) {
        return bytesEnc().mapToIntEncoder(i -> NUM_BYTES[i - 10]);
      } else {
        return bytesEnc().mapToIntEncoder(RespEncoders::toBytes);
      }
    });
  }

  static byte[] toBytes(long num) {
    if (num == Long.MIN_VALUE) return MIN_LONG_BYTES;
    boolean neg = num < 0;
    if (neg) {
      num = -num;
    }
    int size = neg ? stringSize(num) + 1 : stringSize(num);
    byte[] buf = new byte[size];
    if (neg) {
      buf[0] = '-';
    }
    int i = size - 1;
    while (num != 0) {
      buf[i--] = (byte) ('0' + num % 10);
      num /= 10;
    }
    return buf;
  }

  private static int stringSize(long x) {
    for (int i = 0; i < SIZE_TABLE.length; i++) {
      if (x <= SIZE_TABLE[i]) {
        return i + 1;
      }
    }
    return 19;
  }
}

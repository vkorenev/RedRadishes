package redradishes.decoder;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class BulkStringBuilders {
  private static final BulkStringBuilderFactory<?, byte[]> BYTE_ARRAY_BUILDER_FACTORY =
      (SimpleBulkStringBuilderFactory<byte[]>) (length, charsetDecoder) -> new SimpleBulkStringBuilderFactory
          .Builder<byte[]>() {
        private final byte[] bytes = new byte[length];
        private int offset = 0;

        @Override
        public SimpleBulkStringBuilderFactory.Builder<byte[]> append(ByteBuffer buffer) {
          int len = buffer.remaining();
          buffer.get(bytes, offset, len);
          offset += len;
          return this;
        }

        @Override
        public byte[] build() {
          return bytes;
        }
      };

  private static final BulkStringBuilderFactory<?, CharSequence> CHAR_SEQUENCE_BUILDER_FACTORY =
      new BulkStringBuilderFactory<CharBuffer, CharSequence>() {
        @Override
        public CharBuffer create(int length, CharsetDecoder charsetDecoder) {
          return CharBuffer.allocate((int) (length * charsetDecoder.maxCharsPerByte()));
        }

        @Override
        public CharBuffer append(CharBuffer charBuffer, ByteBuffer buffer, CharsetDecoder charsetDecoder)
            throws CharacterCodingException {
          checkResult(charsetDecoder.decode(buffer, charBuffer, false));
          return charBuffer;
        }

        @Override
        public CharSequence appendLast(CharBuffer charBuffer, ByteBuffer buffer, CharsetDecoder charsetDecoder)
            throws CharacterCodingException {
          checkResult(charsetDecoder.decode(buffer, charBuffer, true));
          checkResult(charsetDecoder.flush(charBuffer));
          charsetDecoder.reset();
          charBuffer.flip();
          return charBuffer;
        }

        private void checkResult(CoderResult coderResult) throws CharacterCodingException {
          if (!coderResult.isUnderflow()) {
            coderResult.throwException();
          }
        }
      };

  private static final BulkStringBuilderFactory<?, String> STRING_BUILDER_FACTORY =
      CHAR_SEQUENCE_BUILDER_FACTORY.map(Object::toString);

  private static final int SIGN_OR_DIGIT = 0;
  private static final int DIGIT = 1;
  private static final BulkStringBuilderFactory<?, Long> LONG_BUILDER_FACTORY =
      (SimpleBulkStringBuilderFactory<Long>) (length, charsetDecoder) -> new SimpleBulkStringBuilderFactory
          .Builder<Long>() {
        int state = SIGN_OR_DIGIT;
        boolean negative = false;
        long num = 0;

        @Override
        public SimpleBulkStringBuilderFactory.Builder<Long> append(ByteBuffer buffer) {
          while (buffer.hasRemaining()) {
            byte b = buffer.get();
            switch (state) {
              case SIGN_OR_DIGIT:
                state = DIGIT;
                if (b == '-') {
                  negative = true;
                  break;
                }
              case DIGIT:
                switch (b) {
                  case '0':
                    num *= 10;
                    break;
                  case '1':
                    num = num * 10 + 1;
                    break;
                  case '2':
                    num = num * 10 + 2;
                    break;
                  case '3':
                    num = num * 10 + 3;
                    break;
                  case '4':
                    num = num * 10 + 4;
                    break;
                  case '5':
                    num = num * 10 + 5;
                    break;
                  case '6':
                    num = num * 10 + 6;
                    break;
                  case '7':
                    num = num * 10 + 7;
                    break;
                  case '8':
                    num = num * 10 + 8;
                    break;
                  case '9':
                    num = num * 10 + 9;
                    break;
                  default:
                    throw new IllegalStateException("Unexpected character: " + (char) b);
                }
                break;
            }
          }
          return this;
        }

        @Override
        public Long build() {
          return negative ? -num : num;
        }
      };

  private static final BulkStringBuilderFactory<?, Integer> INTEGER_BUILDER_FACTORY =
      LONG_BUILDER_FACTORY.map(Long::intValue);

  public static BulkStringBuilderFactory<?, CharSequence> charSequence() {
    return CHAR_SEQUENCE_BUILDER_FACTORY;
  }

  public static BulkStringBuilderFactory<?, String> string() {
    return STRING_BUILDER_FACTORY;
  }

  public static BulkStringBuilderFactory<?, Integer> integer() {
    return INTEGER_BUILDER_FACTORY;
  }

  public static BulkStringBuilderFactory<?, Long> _long() {
    return LONG_BUILDER_FACTORY;
  }

  public static BulkStringBuilderFactory<?, byte[]> byteArray() {
    return BYTE_ARRAY_BUILDER_FACTORY;
  }
}

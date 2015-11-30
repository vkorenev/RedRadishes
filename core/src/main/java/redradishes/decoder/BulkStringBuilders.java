package redradishes.decoder;

import redradishes.UncheckedCharacterCodingException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CoderResult;

public class BulkStringBuilders {
  private static final BulkStringBuilderFactory<byte[]> BYTE_ARRAY_BUILDER_FACTORY =
      (length, charsetDecoder) -> new BulkStringBuilderFactory.Builder<byte[]>() {
        private final byte[] bytes = new byte[length];
        private int offset = 0;

        @Override
        public void append(ByteBuffer buffer) {
          int len = buffer.remaining();
          buffer.get(bytes, offset, len);
          offset += len;
        }

        @Override
        public void appendLast(ByteBuffer buffer) {
          append(buffer);
        }

        @Override
        public byte[] build() {
          return bytes;
        }
      };

  private static final BulkStringBuilderFactory<CharSequence> CHAR_SEQUENCE_BUILDER_FACTORY =
      (length, charsetDecoder) -> new BulkStringBuilderFactory.Builder<CharSequence>() {
        private final CharBuffer charBuffer = CharBuffer.allocate((int) (length * charsetDecoder.maxCharsPerByte()));

        @Override
        public void append(ByteBuffer buffer) {
          checkResult(charsetDecoder.decode(buffer, charBuffer, false));
        }

        @Override
        public void appendLast(ByteBuffer buffer) {
          checkResult(charsetDecoder.decode(buffer, charBuffer, true));
          checkResult(charsetDecoder.flush(charBuffer));
          charsetDecoder.reset();
        }

        @Override
        public CharSequence build() {
          charBuffer.flip();
          return charBuffer;
        }

        private void checkResult(CoderResult coderResult) {
          if (!coderResult.isUnderflow()) {
            try {
              coderResult.throwException();
            } catch (CharacterCodingException e) {
              throw new UncheckedCharacterCodingException(e);
            }
          }
        }
      };

  private static final BulkStringBuilderFactory<String> STRING_BUILDER_FACTORY =
      CHAR_SEQUENCE_BUILDER_FACTORY.map(Object::toString);

  private static final int SIGN_OR_DIGIT = 0;
  private static final int DIGIT = 1;
  private static final BulkStringBuilderFactory<Long> LONG_BUILDER_FACTORY =
      (length, charsetDecoder) -> new BulkStringBuilderFactory.Builder<Long>() {
        int state = SIGN_OR_DIGIT;
        boolean negative = false;
        long num = 0;

        @Override
        public void append(ByteBuffer buffer) {
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
        }

        @Override
        public void appendLast(ByteBuffer buffer) {
          append(buffer);
        }

        @Override
        public Long build() {
          return negative ? -num : num;
        }
      };

  private static final BulkStringBuilderFactory<Integer> INTEGER_BUILDER_FACTORY =
      LONG_BUILDER_FACTORY.map(Long::intValue);

  public static BulkStringBuilderFactory<CharSequence> charSequence() {
    return CHAR_SEQUENCE_BUILDER_FACTORY;
  }

  public static BulkStringBuilderFactory<String> string() {
    return STRING_BUILDER_FACTORY;
  }

  public static BulkStringBuilderFactory<Integer> integer() {
    return INTEGER_BUILDER_FACTORY;
  }

  public static BulkStringBuilderFactory<Long> _long() {
    return LONG_BUILDER_FACTORY;
  }

  public static BulkStringBuilderFactory<byte[]> byteArray() {
    return BYTE_ARRAY_BUILDER_FACTORY;
  }
}

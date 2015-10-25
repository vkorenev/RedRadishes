package redradishes.decoder;

import redradishes.UncheckedCharacterCodingException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CoderResult;

public class BulkStringBuilders {
  private static final BulkStringBuilderFactory<byte[]> BYTE_ARRAY_BUILDER_FACTORY = (length, charsetDecoder) -> {
    byte[] bytes = new byte[length];
    return new BulkStringBuilderFactory.Builder<byte[]>() {
      int offset = 0;

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
  };
  private static final BulkStringBuilderFactory<CharSequence> CHAR_SEQUENCE_BUILDER_FACTORY = charSequence();
  private static final BulkStringBuilderFactory<Integer> INTEGER_BUILDER_FACTORY =
      CHAR_SEQUENCE_BUILDER_FACTORY.map(Object::toString).map(Integer::valueOf);
  private static final BulkStringBuilderFactory<Long> LONG_BUILDER_FACTORY =
      CHAR_SEQUENCE_BUILDER_FACTORY.map(Object::toString).map(Long::valueOf);

  public static BulkStringBuilderFactory<CharSequence> charSequence() {
    return (length, charsetDecoder) -> {
      CharBuffer charBuffer = CharBuffer.allocate((int) (length * charsetDecoder.maxCharsPerByte()));
      return new BulkStringBuilderFactory.Builder<CharSequence>() {
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
    };
  }

  public static BulkStringBuilderFactory<String> string() {
    return charSequence().map(Object::toString);
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

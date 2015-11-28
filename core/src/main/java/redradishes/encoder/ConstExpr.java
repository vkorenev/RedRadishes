package redradishes.encoder;

import com.google.common.base.Utf8;
import redradishes.UncheckedCharacterCodingException;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static redradishes.encoder.RespEncoders.getCharsetEncoder;

public interface ConstExpr {
  ConstExpr EMPTY = new ConstExpr() {
    @Override
    public int length() {
      return 0;
    }

    @Override
    public void writeTo(ByteSink byteSink) { }

    @Override
    public int size() {
      return 0;
    }
  };

  ConstExpr NEW_ARG = new ConstExpr() {
    @Override
    public int length() {
      return 1;
    }

    @Override
    public void writeTo(ByteSink byteSink) {
      byteSink.write((byte) '$');
    }

    @Override
    public int size() {
      return 1;
    }
  };

  int length();

  void writeTo(ByteSink byteSink);

  int size();

  default ConstExpr compact() {
    return this;
  }

  default <E extends EncoderBase<E>> E append(E enc) {
    return enc.prepend(this);
  }

  default ConstExpr append(ConstExpr c) {
    return combine(this, c);
  }

  static ConstExpr byteConst(byte b) {
    return new ConstExpr() {
      @Override
      public int length() {
        return 1;
      }

      @Override
      public void writeTo(ByteSink byteSink) {
        byteSink.write(b);
      }

      @Override
      public int size() {
        return 0;
      }
    };
  }

  static ConstExpr bytesConst(byte[] src) {
    return bytesConstWithSize(src, 0);
  }

  static ConstExpr bytesConstWithSize(byte[] src, int size) {
    return new ConstExpr() {
      @Override
      public int length() {
        return src.length;
      }

      @Override
      public void writeTo(ByteSink byteSink) {
        byteSink.write(src);
      }

      @Override
      public int size() {
        return size;
      }
    };
  }

  static ConstExpr bytesConst(byte[] src, int offset, int length) {
    return new ConstExpr() {
      @Override
      public int length() {
        return length;
      }

      @Override
      public void writeTo(ByteSink byteSink) {
        byteSink.write(src, offset, length);
      }

      @Override
      public int size() {
        return 0;
      }
    };
  }

  static ConstExpr strConst(CharSequence s, Charset charset) {
    return new ConstExpr() {
      @Override
      public int length() {
        if (s.length() == 0) {
          return 0;
        } else if (UTF_8.equals(charset)) {
          return Utf8.encodedLength(s);
        } else {
          CharsetEncoder charsetEncoder = getCharsetEncoder(charset);
          if (charsetEncoder.maxBytesPerChar() == 1.0) {
            return s.length();
          } else {
            CharBuffer charBuffer = CharBuffer.wrap(s);
            try {
              return charsetEncoder.encode(charBuffer).remaining();
            } catch (CharacterCodingException e) {
              throw new UncheckedCharacterCodingException(e);
            }
          }
        }
      }

      @Override
      public void writeTo(ByteSink byteSink) {
        byteSink.write(s, getCharsetEncoder(charset));
      }

      @Override
      public int size() {
        return 0;
      }

      @Override
      public ConstExpr compact() {
        try {
          ByteBuffer byteBuffer = getCharsetEncoder(charset).encode(CharBuffer.wrap(s));
          return bytesConst(byteBuffer.array(), 0, byteBuffer.remaining());
        } catch (CharacterCodingException e) {
          throw new UncheckedCharacterCodingException(e);
        }
      }
    };
  }

  static ConstExpr combine(ConstExpr c1, ConstExpr c2) {
    return new ConstExpr() {
      @Override
      public int length() {
        return c1.length() + c2.length();
      }

      @Override
      public void writeTo(ByteSink byteSink) {
        c1.writeTo(byteSink);
        c2.writeTo(byteSink);
      }

      @Override
      public int size() {
        return c1.size() + c2.size();
      }

      @Override
      public ConstExpr compact() {
        ByteArrayOutputStream out = new ByteArrayOutputStream(length());
        ByteSink sink = new ByteSink() {
          @Override
          public void write(byte b) {
            out.write(b);
          }

          @Override
          public void write(CharSequence s, CharsetEncoder charsetEncoder) {
            try {
              ByteBuffer byteBuffer = charsetEncoder.encode(CharBuffer.wrap(s));
              out.write(byteBuffer.array(), 0, byteBuffer.remaining());
            } catch (CharacterCodingException e) {
              throw new UncheckedCharacterCodingException(e);
            }
          }

          @Override
          public void write(byte[] src) {
            out.write(src, 0, src.length);
          }

          @Override
          public void write(byte[] src, int offset, int length) {
            out.write(src, offset, length);
          }
        };
        c1.writeTo(sink);
        c2.writeTo(sink);
        byte[] bytes = out.toByteArray();
        return bytesConstWithSize(bytes, size());
      }
    };
  }
}

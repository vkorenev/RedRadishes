package redradishes.encoder;

import java.nio.charset.CharsetEncoder;

public interface ConstExpr {
  ConstExpr EMPTY = byteSink -> {
  };

  void writeTo(ByteSink byteSink);

  default int size() {
    return 0;
  }

  default <E extends EncoderBase<E>> E append(E enc) {
    return enc.prepend(this);
  }

  default ConstExpr append(ConstExpr c) {
    return combine(this, c);
  }

  default <T> Encoder<T> constEnc() {
    return val -> ConstExpr.this;
  }

  static ConstExpr byteConst(byte b) {
    return byteSink -> byteSink.write(b);
  }

  static ConstExpr bytesConst(byte[] src) {
    return byteSink -> byteSink.write(src);
  }

  static ConstExpr bytesConst(byte[] src, int offset, int length) {
    return byteSink -> byteSink.write(src, offset, length);
  }

  static ConstExpr newArg() {
    return new ConstExpr() {
      @Override
      public void writeTo(ByteSink byteSink) {
        byteSink.write((byte) '$');
      }

      @Override
      public int size() {
        return 1;
      }
    };
  }

  static ConstExpr strConst(CharSequence s, CharsetEncoder charsetEncoder) {
    return byteSink -> byteSink.write(s, charsetEncoder);
  }

  static ConstExpr combine(ConstExpr c1, ConstExpr c2) {
    return new ConstExpr() {
      @Override
      public void writeTo(ByteSink byteSink) {
        c1.writeTo(byteSink);
        c2.writeTo(byteSink);
      }

      @Override
      public int size() {
        return c1.size() + c2.size();
      }
    };
  }
}

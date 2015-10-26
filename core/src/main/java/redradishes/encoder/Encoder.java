package redradishes.encoder;

import java.nio.charset.CharsetEncoder;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public interface Encoder<T> extends EncoderBase<Encoder<T>> {
  ConstExpr encode(T val);

  @Override
  default Encoder<T> prepend(ConstExpr c) {
    return new Encoder<T>() {
      @Override
      public ConstExpr encode(T val) {
        return ConstExpr.combine(c, Encoder.this.encode(val));
      }

      @Override
      public Encoder<T> compact() {
        return Encoder.this.compact().prepend(c.compact());
      }
    };
  }

  @Override
  default Encoder<T> append(ConstExpr c) {
    return new Encoder<T>() {
      @Override
      public ConstExpr encode(T val) {
        return ConstExpr.combine(Encoder.this.encode(val), c);
      }

      @Override
      public Encoder<T> compact() {
        return Encoder.this.compact().append(c.compact());
      }
    };
  }

  @Override
  default Encoder<T> compact() {
    return this;
  }

  default <T2> Encoder2<T, T2> append(Encoder<T2> other) {
    return new Encoder2<T, T2>() {
      @Override
      public ConstExpr encode(T val1, T2 val2) {
        return Encoder.this.encode(val1).append(other.encode(val2));
      }

      @Override
      public Encoder2<T, T2> compact() {
        return Encoder.this.compact().append(other.compact());
      }
    };
  }

  default Encoder<T> zip(Encoder<T> enc2) {
    return new Encoder<T>() {
      @Override
      public ConstExpr encode(T val) {
        return Encoder.this.encode(val).append(enc2.encode(val));
      }

      @Override
      public Encoder<T> compact() {
        return Encoder.this.compact().zip(enc2.compact());
      }
    };
  }

  default IntEncoder mapToIntEncoder(IntFunction<? extends T> mapper) {
    return new IntEncoder() {
      @Override
      public ConstExpr encode(int val) {
        return Encoder.this.encode(mapper.apply(val));
      }

      @Override
      public IntEncoder compact() {
        return Encoder.this.compact().mapToIntEncoder(mapper);
      }
    };
  }

  static Encoder<byte[]> bytesEnc() {
    return ConstExpr::bytesConst;
  }

  static Encoder<CharSequence> stringEnc(CharsetEncoder charsetEncoder) {
    return val -> ConstExpr.strConst(val, charsetEncoder);
  }

  static <T> Encoder<T> choice(Predicate<T> predicate, Encoder<T> then, Encoder<T> otherwise) {
    return new Encoder<T>() {
      @Override
      public ConstExpr encode(T val) {
        return predicate.test(val) ? then.encode(val) : otherwise.encode(val);
      }

      @Override
      public Encoder<T> compact() {
        return choice(predicate, then.compact(), otherwise.compact());
      }
    };
  }

  static <T> Encoder<T> choiceConst(Predicate<T> predicate, ConstExpr then, Encoder<T> otherwise) {
    return new Encoder<T>() {
      @Override
      public ConstExpr encode(T val) {
        return predicate.test(val) ? then : otherwise.encode(val);
      }

      @Override
      public Encoder<T> compact() {
        return choiceConst(predicate, then.compact(), otherwise.compact());
      }
    };
  }
}

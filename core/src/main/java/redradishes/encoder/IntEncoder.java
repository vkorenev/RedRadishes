package redradishes.encoder;

import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

import static redradishes.encoder.ConstExpr.byteConst;

public interface IntEncoder extends EncoderBase<IntEncoder> {
  ConstExpr encode(int val);

  @Override
  default IntEncoder prepend(ConstExpr c) {
    return new IntEncoder() {
      @Override
      public ConstExpr encode(int val) {
        return ConstExpr.combine(c, IntEncoder.this.encode(val));
      }

      @Override
      public IntEncoder compact() {
        return IntEncoder.this.compact().prepend(c.compact());
      }
    };
  }

  @Override
  default IntEncoder append(ConstExpr c) {
    return new IntEncoder() {
      @Override
      public ConstExpr encode(int val) {
        return ConstExpr.combine(IntEncoder.this.encode(val), c);
      }

      @Override
      public IntEncoder compact() {
        return IntEncoder.this.compact().append(c.compact());
      }
    };
  }

  @Override
  default IntEncoder compact() {
    return this;
  }

  default <U> Encoder<U> map(ToIntFunction<? super U> mapper) {
    return new Encoder<U>() {
      @Override
      public ConstExpr encode(U val) {
        return IntEncoder.this.encode(mapper.applyAsInt(val));
      }

      @Override
      public Encoder<U> compact() {
        return IntEncoder.this.compact().map(mapper);
      }
    };
  }

  static IntEncoder digitEncoder() {
    return i -> byteConst((byte) ('0' + i));
  }

  static IntEncoder choice(IntPredicate predicate, IntEncoder then, IntEncoder otherwise) {
    return new IntEncoder() {
      @Override
      public ConstExpr encode(int val) {
        return predicate.test(val) ? then.encode(val) : otherwise.encode(val);
      }

      @Override
      public IntEncoder compact() {
        return choice(predicate, then.compact(), otherwise.compact());
      }
    };
  }
}

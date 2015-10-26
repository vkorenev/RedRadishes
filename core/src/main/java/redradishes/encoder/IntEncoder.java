package redradishes.encoder;

import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

public interface IntEncoder extends EncoderBase<IntEncoder> {
  ConstExpr encode(int val);

  @Override
  default IntEncoder prepend(ConstExpr c) {
    return val -> ConstExpr.combine(c, IntEncoder.this.encode(val));
  }

  @Override
  default IntEncoder append(ConstExpr c) {
    return val -> ConstExpr.combine(IntEncoder.this.encode(val), c);
  }

  default IntEncoder mapToIntEncoder(IntUnaryOperator mapper) {
    return val -> IntEncoder.this.encode(mapper.applyAsInt(val));
  }

  default <U> Encoder<U> map(ToIntFunction<? super U> mapper) {
    return val -> IntEncoder.this.encode(mapper.applyAsInt(val));
  }

  static IntEncoder byteEnc() {
    return i -> ConstExpr.byteConst((byte) i);
  }

  static IntEncoder flatMap(IntFunction<IntEncoder> mapper) {
    return val -> mapper.apply(val).encode(val);
  }
}

package redradishes.encoder;

import java.util.function.Function;
import java.util.function.IntFunction;

public interface Encoder<T> extends EncoderBase<Encoder<T>> {
  ConstExpr encode(T val);

  @Override
  default Encoder<T> prepend(ConstExpr c) {
    return val -> ConstExpr.combine(c, Encoder.this.encode(val));
  }

  @Override
  default Encoder<T> append(ConstExpr c) {
    return val -> ConstExpr.combine(Encoder.this.encode(val), c);
  }

  default <T2> Encoder2<T, T2> append(Encoder<T2> other) {
    return (val1, val2) -> Encoder.this.encode(val1).append(other.encode(val2));
  }

  default Encoder<T> zip(Encoder<T> enc2) {
    return val -> Encoder.this.encode(val).append(enc2.encode(val));
  }

  default <U> Encoder<U> map(Function<? super U, ? extends T> mapper) {
    return val -> Encoder.this.encode(mapper.apply(val));
  }

  default IntEncoder mapToIntEncoder(IntFunction<? extends T> mapper) {
    return val -> Encoder.this.encode(mapper.apply(val));
  }

  static Encoder<byte[]> bytesEnc() {
    return ConstExpr::bytesConst;
  }

  static <T> Encoder<T> flatMap(Function<? super T, Encoder<T>> mapper) {
    return val -> mapper.apply(val).encode(val);
  }
}

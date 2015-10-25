package redradishes.encoder;

public interface Encoder2<T1, T2> extends EncoderBase<Encoder2<T1, T2>> {
  ConstExpr encode(T1 val1, T2 val2);

  @Override
  default Encoder2<T1, T2> prepend(ConstExpr c) {
    return (val1, val2) -> ConstExpr.combine(c, Encoder2.this.encode(val1, val2));
  }

  @Override
  default Encoder2<T1, T2> append(ConstExpr c) {
    return (val1, val2) -> ConstExpr.combine(Encoder2.this.encode(val1, val2), c);
  }

  default <T3> Encoder3<T1, T2, T3> append(Encoder<T3> other) {
    return (val1, val2, val3) -> Encoder2.this.encode(val1, val2).append(other.encode(val3));
  }
}

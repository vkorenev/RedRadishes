package redradishes.encoder;

public interface Encoder2<T1, T2> extends EncoderBase<Encoder2<T1, T2>> {
  ConstExpr encode(T1 val1, T2 val2);

  @Override
  default Encoder2<T1, T2> prepend(ConstExpr c) {
    return new Encoder2<T1, T2>() {
      @Override
      public ConstExpr encode(T1 val1, T2 val2) {
        return ConstExpr.combine(c, Encoder2.this.encode(val1, val2));
      }

      @Override
      public Encoder2<T1, T2> compact() {
        return Encoder2.this.compact().prepend(c.compact());
      }
    };
  }

  @Override
  default Encoder2<T1, T2> append(ConstExpr c) {
    return new Encoder2<T1, T2>() {
      @Override
      public ConstExpr encode(T1 val1, T2 val2) {
        return ConstExpr.combine(Encoder2.this.encode(val1, val2), c);
      }

      @Override
      public Encoder2<T1, T2> compact() {
        return Encoder2.this.compact().append(c.compact());
      }
    };
  }

  @Override
  default Encoder2<T1, T2> compact() {
    return this;
  }

  default <T3> Encoder3<T1, T2, T3> append(Encoder<T3> other) {
    return new Encoder3<T1, T2, T3>() {
      @Override
      public ConstExpr encode(T1 val1, T2 val2, T3 val3) {
        return Encoder2.this.encode(val1, val2).append(other.encode(val3));
      }

      @Override
      public Encoder3<T1, T2, T3> compact() {
        return Encoder2.this.compact().append(other.compact());
      }
    };
  }
}

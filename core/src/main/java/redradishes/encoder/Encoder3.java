package redradishes.encoder;

public interface Encoder3<T1, T2, T3> extends EncoderBase<Encoder3<T1, T2, T3>> {
  ConstExpr encode(T1 val1, T2 val2, T3 val3);

  @Override
  default Encoder3<T1, T2, T3> prepend(ConstExpr c) {
    return new Encoder3<T1, T2, T3>() {
      @Override
      public ConstExpr encode(T1 val1, T2 val2, T3 val3) {
        return ConstExpr.combine(c, Encoder3.this.encode(val1, val2, val3));
      }

      @Override
      public Encoder3<T1, T2, T3> compact() {
        return Encoder3.this.compact().prepend(c.compact());
      }
    };
  }

  @Override
  default Encoder3<T1, T2, T3> append(ConstExpr c) {
    return new Encoder3<T1, T2, T3>() {
      @Override
      public ConstExpr encode(T1 val1, T2 val2, T3 val3) {
        return ConstExpr.combine(Encoder3.this.encode(val1, val2, val3), c);
      }

      @Override
      public Encoder3<T1, T2, T3> compact() {
        return Encoder3.this.compact().append(c.compact());
      }
    };
  }

  @Override
  default Encoder3<T1, T2, T3> compact() {
    return this;
  }
}

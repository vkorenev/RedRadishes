package redradishes.encoder;

public interface EncoderBase<E extends EncoderBase<E>> {
  E prepend(ConstExpr c);

  E append(ConstExpr c);
}

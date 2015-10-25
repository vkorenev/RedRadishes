package redradishes.encoder;

import redradishes.Request;

public interface Command<T> extends Request<T> {
  ConstExpr c();

  @Override
  default void writeTo(ByteSink sink) {
    ConstExpr params = c();
    ConstExpr expr = RespEncoders.array().encode(params.size()).append(params);
    expr.writeTo(sink);
  }
}

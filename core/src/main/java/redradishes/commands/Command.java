package redradishes.commands;

import redradishes.Request;
import redradishes.encoder.ByteSink;
import redradishes.encoder.ConstExpr;
import redradishes.encoder.RespEncoders;

public interface Command<T> extends Request<T> {
  ConstExpr c();

  @Override
  default void writeTo(ByteSink sink) {
    ConstExpr params = c();
    ConstExpr expr = RespEncoders.array().encode(params.size()).append(params);
    expr.writeTo(sink);
  }
}

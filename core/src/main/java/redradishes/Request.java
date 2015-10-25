package redradishes;

import redradishes.decoder.parser.ReplyParser;
import redradishes.encoder.ByteSink;

public interface Request<T> {
  void writeTo(ByteSink sink);

  ReplyParser<? extends T> parser();
}

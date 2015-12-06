package redradishes;

import redradishes.decoder.parser.ReplyParser;
import redradishes.encoder.ByteSink;

import java.util.function.BiFunction;

public interface Request<T> {
  void writeTo(ByteSink sink);

  ReplyParser<? extends T> parser();

  default <U, V> Request<V> combine(Request<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
    return new Request<V>() {
      @Override
      public void writeTo(ByteSink sink) {
        Request.this.writeTo(sink);
        other.writeTo(sink);
      }

      @Override
      public ReplyParser<V> parser() {
        return ReplyParser.combine(Request.this.parser(), other.parser(), fn);
      }
    };
  }

  default <U> Request<T> combineIgnoringFirst(Request<U> other) {
    return combine(other, (a, b) -> a);
  }

  default <U> Request<U> combineIgnoringSecond(Request<U> other) {
    return combine(other, (a, b) -> b);
  }
}

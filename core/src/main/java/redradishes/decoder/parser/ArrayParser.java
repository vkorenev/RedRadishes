package redradishes.decoder.parser;

import redradishes.decoder.ArrayBuilderFactory;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.function.Function;

public class ArrayParser<T, E> implements ReplyParser<T> {
  private final int len;
  private final ArrayBuilderFactory<E, ? extends T> builderFactory;
  private final ReplyParser<E> elementParser;

  public ArrayParser(int len, ArrayBuilderFactory<E, ? extends T> builderFactory, ReplyParser<E> elementParser) {
    this.len = len;
    this.builderFactory = builderFactory;
    this.elementParser = elementParser;
  }

  @Override
  public <U> U parseReply(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      CharsetDecoder charsetDecoder) {
    return doParse(buffer, resultHandler, partialReplyHandler, failureHandler, builderFactory.create(len), len,
        elementParser, charsetDecoder);
  }

  private <U> U doParse(ByteBuffer buffer, Function<? super T, U> resultHandler,
      PartialReplyHandler<? super T, U> partialReplyHandler, FailureHandler<U> failureHandler,
      ArrayBuilderFactory.Builder<E, ? extends T> builder, int remaining, ReplyParser<? extends E> elemParser,
      CharsetDecoder charsetDecoder) {
    State state = new State(remaining, elemParser);
    while (state.remaining > 0) {
      ReplyParser<T> partialParser = state.elemParser.parseReply(buffer, value -> {
        builder.add(value);
        state.remaining--;
        state.elemParser = elementParser;
        return null;
      }, partial -> partialParser(builder, state.remaining, partial), e -> {
        state.remaining--;
        state.elemParser = elementParser.fail(e);
        state.throwable = e;
        return null;
      }, charsetDecoder);
      if (partialParser != null) {
        return partialReplyHandler.partialReply(partialParser);
      }
    }
    if (state.throwable != null) {
      return failureHandler.failure(state.throwable);
    } else {
      return resultHandler.apply(builder.build());
    }
  }

  private ReplyParser<T> partialParser(ArrayBuilderFactory.Builder<E, ? extends T> builder, int remaining,
      ReplyParser<? extends E> partial) {
    return new ReplyParser<T>() {
      @Override
      public <U1> U1 parseReply(ByteBuffer buffer, Function<? super T, U1> resultHandler,
          PartialReplyHandler<? super T, U1> partialReplyHandler, FailureHandler<U1> failureHandler,
          CharsetDecoder charsetDecoder) {
        return doParse(buffer, resultHandler, partialReplyHandler, failureHandler, builder, remaining, partial,
            charsetDecoder);
      }
    };
  }

  private class State {
    int remaining;
    ReplyParser<? extends E> elemParser;
    Throwable throwable;

    public State(int remaining, ReplyParser<? extends E> elemParser) {
      this.remaining = remaining;
      this.elemParser = elemParser;
    }
  }
}

package redradishes;

import redradishes.decoder.parser.ReplyParser;
import redradishes.encoder.ByteSink;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class CommandList<T> implements Request<List<T>> {
  private final List<Request<T>> requests;

  public CommandList(List<Request<T>> requests) {
    this.requests = requests;
  }

  @Override
  public void writeTo(ByteSink sink) {
    for (Request<T> request : requests) {
      request.writeTo(sink);
    }
  }

  @Override
  public ReplyParser<List<T>> parser() {
    return new ReplyParser<List<T>>() {
      private final List<T> replies = new ArrayList<>(requests.size());
      private final Iterator<Request<T>> requestIterator = requests.iterator();

      @Override
      public <U> U parseReply(ByteBuffer buffer, Function<? super List<T>, U> resultHandler,
          PartialReplyHandler<? super List<T>, U> partialReplyHandler, FailureHandler<U> failureHandler,
          CharsetDecoder charsetDecoder) {
        if (requestIterator.hasNext()) {
          return doParse(buffer, resultHandler, partialReplyHandler, failureHandler, requestIterator.next().parser(),
              charsetDecoder);
        } else {
          return resultHandler.apply(replies);
        }
      }

      private <U> U doParse(ByteBuffer buffer, Function<? super List<T>, U> resultHandler,
          PartialReplyHandler<? super List<T>, U> partialReplyHandler, FailureHandler<U> failureHandler,
          ReplyParser<? extends T> parser, CharsetDecoder charsetDecoder) {
        return parser.parseReply(buffer, value -> {
          replies.add(value);
          return parseReply(buffer, resultHandler, partialReplyHandler, failureHandler, charsetDecoder);
        }, partial -> partialReplyHandler.partialReply(new ReplyParser<List<T>>() {
          @Override
          public <U1> U1 parseReply(ByteBuffer buffer, Function<? super List<T>, U1> resultHandler,
              PartialReplyHandler<? super List<T>, U1> partialReplyHandler, FailureHandler<U1> failureHandler,
              CharsetDecoder charsetDecoder) {
            return doParse(buffer, resultHandler, partialReplyHandler, failureHandler, partial, charsetDecoder);
          }
        }), failureHandler, charsetDecoder);
      }
    };
  }
}

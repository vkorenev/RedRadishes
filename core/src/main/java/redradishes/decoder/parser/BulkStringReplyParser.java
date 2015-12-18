package redradishes.decoder.parser;

import java.util.function.IntFunction;

public class BulkStringReplyParser<T> extends AnyReplyParser<T> {
  public BulkStringReplyParser(IntFunction<ReplyParser<T>> bodyParserFactory) {
    super(new UnexpectedSimpleReplyParser<>("simple string"), new ErrorParser<>(),
        new UnexpectedSimpleReplyParser<>("integer"), new LenParser<>(bodyParserFactory),
        new UnexpectedArrayReplyParser<>());
  }
}

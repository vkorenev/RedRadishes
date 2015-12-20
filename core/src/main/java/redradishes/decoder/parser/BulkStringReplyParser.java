package redradishes.decoder.parser;

import java.util.function.IntFunction;

import static redradishes.decoder.parser.ErrorParser.errorParser;

public class BulkStringReplyParser<T> extends AnyReplyParser<T> {
  public BulkStringReplyParser(IntFunction<ReplyParser<T>> bodyParserFactory) {
    super(new UnexpectedSimpleReplyParser<>("simple string"), errorParser(),
        new UnexpectedSimpleReplyParser<>("integer"), new LenParser<>(bodyParserFactory),
        new UnexpectedArrayReplyParser<>());
  }
}

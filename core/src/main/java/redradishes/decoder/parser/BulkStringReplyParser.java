package redradishes.decoder.parser;

import java.util.function.IntFunction;

import static redradishes.decoder.parser.ErrorParser.errorParser;

public class BulkStringReplyParser<T> extends AnyReplyParser<T> {
  private static final UnexpectedReplyTypeParsers UNEXPECTED = new UnexpectedReplyTypeParsers("bulk string reply");

  public BulkStringReplyParser(IntFunction<ReplyParser<T>> bodyParserFactory) {
    super(UNEXPECTED.simpleStringParser(), errorParser(), UNEXPECTED.integerParser(),
        new LenParser<>(bodyParserFactory), UNEXPECTED.arrayParser());
  }
}

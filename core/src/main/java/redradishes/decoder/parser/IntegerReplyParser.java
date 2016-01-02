package redradishes.decoder.parser;

import static redradishes.decoder.parser.ErrorParser.errorParser;

public class IntegerReplyParser<T> extends AnyReplyParser<T> {
  private static final UnexpectedReplyTypeParsers UNEXPECTED = new UnexpectedReplyTypeParsers("integer");

  public IntegerReplyParser(Parser<T> parser) {
    super(UNEXPECTED.simpleStringParser(), errorParser(), parser, UNEXPECTED.nilBulkStringParser(),
        UNEXPECTED.arrayParser());
  }
}

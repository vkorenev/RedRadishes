package redradishes.decoder.parser;

import static redradishes.decoder.parser.ErrorParser.errorParser;
import static redradishes.decoder.parser.ExpectedResultParser.nilParser;

public class IntegerReplyParser<T> extends AnyReplyParser<T> {
  private static final UnexpectedReplyTypeParsers UNEXPECTED = new UnexpectedReplyTypeParsers("integer reply");

  public IntegerReplyParser(Parser<T> parser) {
    super(UNEXPECTED.simpleStringParser(), errorParser(), parser, nilParser(), new UnexpectedArrayReplyParser<>());
  }
}

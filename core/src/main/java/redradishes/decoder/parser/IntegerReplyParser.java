package redradishes.decoder.parser;

import static redradishes.decoder.parser.ErrorParser.errorParser;
import static redradishes.decoder.parser.ExpectedResultParser.nilParser;

public class IntegerReplyParser<T> extends AnyReplyParser<T> {
  public IntegerReplyParser(Parser<T> parser) {
    super(new UnexpectedSimpleReplyParser<>("simple string"), errorParser(), parser, nilParser(),
        new UnexpectedArrayReplyParser<>());
  }
}

package redradishes.decoder.parser;

import static redradishes.decoder.parser.ErrorParser.errorParser;

public class SimpleStringReplyParser<T> extends AnyReplyParser<T> {
  private static final UnexpectedReplyTypeParsers UNEXPECTED = new UnexpectedReplyTypeParsers("simple string reply");

  public SimpleStringReplyParser(ReplyParser<? extends T> parser) {
    super(parser, errorParser(), UNEXPECTED.integerParser(), UNEXPECTED.nilBulkStringParser(),
        UNEXPECTED.arrayParser());
  }
}

package redradishes.decoder.parser;

import static redradishes.decoder.parser.ExpectedResultParser.nilParser;

public class SimpleStringReplyParser<T> extends AnyReplyParser<T> {
  public SimpleStringReplyParser(ReplyParser<? extends T> parser) {
    super(parser, new ErrorParser<>(), new UnexpectedSimpleReplyParser<>("integer"), nilParser(),
        new UnexpectedArrayReplyParser<>());
  }
}

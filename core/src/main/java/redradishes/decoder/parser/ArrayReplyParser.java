package redradishes.decoder.parser;

import java.util.function.IntFunction;

import static redradishes.decoder.parser.ExpectedResultParser.nilParser;

public class ArrayReplyParser<T> extends AnyReplyParser<T> {
  public ArrayReplyParser(IntFunction<Parser<T>> bodyParserFactory) {
    super(new UnexpectedSimpleReplyParser<>("simple string"), new ErrorParser<>(),
        new UnexpectedSimpleReplyParser<>("integer"), nilParser(), new LenParser<>(bodyParserFactory));
  }
}

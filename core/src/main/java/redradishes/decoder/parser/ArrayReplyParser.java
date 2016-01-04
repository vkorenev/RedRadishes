package redradishes.decoder.parser;

import java.util.function.IntFunction;

import static redradishes.decoder.parser.ErrorParser.errorParser;

public class ArrayReplyParser<T> extends AnyReplyParser<T> {
  private static final UnexpectedReplyTypeParsers UNEXPECTED = new UnexpectedReplyTypeParsers("array");

  public ArrayReplyParser(IntFunction<Parser<T>> bodyParserFactory) {
    super(UNEXPECTED.simpleStringParser(), errorParser(), UNEXPECTED.integerParser(), UNEXPECTED.nilBulkStringParser(),
        new LenParser<>(bodyParserFactory));
  }
}

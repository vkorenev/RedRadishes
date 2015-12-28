package redradishes.decoder.parser;

import java.util.function.IntFunction;

import static redradishes.decoder.parser.ErrorParser.errorParser;
import static redradishes.decoder.parser.ExpectedResultParser.nilParser;

public class ArrayReplyParser<T> extends AnyReplyParser<T> {
  private static final UnexpectedReplyTypeParsers UNEXPECTED = new UnexpectedReplyTypeParsers("array reply");

  public ArrayReplyParser(IntFunction<Parser<T>> bodyParserFactory) {
    super(UNEXPECTED.simpleStringParser(), errorParser(), UNEXPECTED.integerParser(), nilParser(),
        new LenParser<>(bodyParserFactory));
  }
}

package redradishes.decoder.parser;

import java.util.function.IntFunction;

public class BulkStringReplyParser<T> extends SuccessOrFailureParser<T> {
  public BulkStringReplyParser(IntFunction<Parser<T>> bodyParserFactory) {
    super('$', new LenParser<>(bodyParserFactory));
  }
}

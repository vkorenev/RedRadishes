package redradishes.decoder.parser;

import java.util.function.IntFunction;

public class ArrayReplyParser<T> extends SuccessOrFailureParser<T> {
  public ArrayReplyParser(IntFunction<Parser<T>> bodyParserFactory) {
    super('*', new LenParser<>(bodyParserFactory));
  }
}

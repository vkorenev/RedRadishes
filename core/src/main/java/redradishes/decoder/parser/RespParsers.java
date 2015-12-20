package redradishes.decoder.parser;

import java.util.function.IntFunction;

public class RespParsers {
  public static <T> ReplyParser<T> arrayReplyParser(IntFunction<? extends Parser<T>> bodyParserFactory) {
    return new PrefixedParser<>('*', new LenParser<>(bodyParserFactory));
  }
}

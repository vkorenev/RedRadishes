package redradishes.decoder.parser;

import redradishes.decoder.BulkStringBuilderFactory;

import java.util.function.IntFunction;

public class RespParsers {
  public static <T> ReplyParser<T> bulkStringParser(BulkStringBuilderFactory<?, ? extends T> builderFactory) {
    return new PrefixedParser<>('$', new LenParser<>(len -> new BulkStringParser<>(len, builderFactory)));
  }

  public static <T> ReplyParser<T> arrayReplyParser(IntFunction<? extends Parser<T>> bodyParserFactory) {
    return new PrefixedParser<>('*', new LenParser<>(bodyParserFactory));
  }
}

package redradishes.decoder.parser;

import redradishes.decoder.BulkStringBuilderFactory;

public class RespParsers {
  public static <T> Parser<T> bulkStringParser(BulkStringBuilderFactory<? extends T> builderFactory) {
    return new PrefixedParser<>('$', new LenParser<>(len -> new BulkStringParser<>(len, builderFactory)));
  }
}

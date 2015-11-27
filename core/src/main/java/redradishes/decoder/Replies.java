package redradishes.decoder;

import redradishes.decoder.parser.ArrayAsMapParser;
import redradishes.decoder.parser.ArrayParser;
import redradishes.decoder.parser.ArrayReplyParser;
import redradishes.decoder.parser.BulkStringParser;
import redradishes.decoder.parser.BulkStringReplyParser;
import redradishes.decoder.parser.IntegerReplyParser;
import redradishes.decoder.parser.LenParser;
import redradishes.decoder.parser.Parser;
import redradishes.decoder.parser.PrefixedParser;
import redradishes.decoder.parser.SeqParser;
import redradishes.decoder.parser.SimpleStringReplyParser;

import static redradishes.decoder.parser.LongParser.INTEGER_PARSER;
import static redradishes.decoder.parser.LongParser.LONG_PARSER;
import static redradishes.decoder.parser.StringParser.STRING_PARSER;

public class Replies {

  private static final IntegerReplyParser<Integer> INTEGER_REPLY_PARSER = new IntegerReplyParser<>(INTEGER_PARSER);
  private static final IntegerReplyParser<Long> LONG_REPLY_PARSER = new IntegerReplyParser<>(LONG_PARSER);
  private static final SimpleStringReplyParser<CharSequence> SIMPLE_STRING_REPLY_PARSER =
      new SimpleStringReplyParser<>(STRING_PARSER);

  public static IntegerReplyParser<Integer> integerReply() {
    return INTEGER_REPLY_PARSER;
  }

  public static IntegerReplyParser<Long> longReply() {
    return LONG_REPLY_PARSER;
  }

  public static SimpleStringReplyParser<CharSequence> simpleStringReply() {
    return SIMPLE_STRING_REPLY_PARSER;
  }

  public static <T> BulkStringReplyParser<T> bulkStringReply(BulkStringBuilderFactory<? extends T> builderFactory) {
    return new BulkStringReplyParser<>(len -> new BulkStringParser<>(len, builderFactory));
  }

  private static <T> Parser<T> bulkStringReplyNoFail(BulkStringBuilderFactory<? extends T> builderFactory) {
    return new PrefixedParser<>('$', new LenParser<>(len -> new BulkStringParser<>(len, builderFactory)));
  }

  public static <E, T> ArrayReplyParser<T> arrayReply(ArrayBuilderFactory<E, ? extends T> arrayBuilderFactory,
      BulkStringBuilderFactory<? extends E> elementBuilderFactory) {
    Parser<E> elementParser = bulkStringReplyNoFail(elementBuilderFactory);
    return new ArrayReplyParser<>(len -> new ArrayParser<>(len, arrayBuilderFactory, elementParser));
  }

  public static <K, V, T> ArrayReplyParser<T> mapReply(MapBuilderFactory<K, V, ? extends T> arrayBuilderFactory,
      BulkStringBuilderFactory<? extends K> keyBuilderFactory,
      BulkStringBuilderFactory<? extends V> valueBuilderFactory) {
    SeqParser<K, V> kvParser =
        SeqParser.seq(bulkStringReplyNoFail(keyBuilderFactory), bulkStringReplyNoFail(valueBuilderFactory));
    return new ArrayReplyParser<>(len -> new ArrayAsMapParser<>(len / 2, arrayBuilderFactory, kvParser));
  }
}

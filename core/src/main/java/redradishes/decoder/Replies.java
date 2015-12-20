package redradishes.decoder;

import redradishes.decoder.parser.ArrayAsMapParser;
import redradishes.decoder.parser.ArrayParser;
import redradishes.decoder.parser.ArrayReplyParser;
import redradishes.decoder.parser.BulkStringParser;
import redradishes.decoder.parser.BulkStringReplyParser;
import redradishes.decoder.parser.CombiningReplyParser;
import redradishes.decoder.parser.IntegerReplyParser;
import redradishes.decoder.parser.ReplyParser;
import redradishes.decoder.parser.ScanReplyParser;
import redradishes.decoder.parser.SimpleStringReplyParser;

import static redradishes.decoder.parser.CharAppendingParser.CHAR_SEQUENCE_PARSER;
import static redradishes.decoder.parser.LongParser.INTEGER_PARSER;
import static redradishes.decoder.parser.LongParser.LONG_PARSER;

public class Replies {

  private static final IntegerReplyParser<Integer> INTEGER_REPLY_PARSER = new IntegerReplyParser<>(INTEGER_PARSER);
  private static final IntegerReplyParser<Long> LONG_REPLY_PARSER = new IntegerReplyParser<>(LONG_PARSER);
  private static final SimpleStringReplyParser<CharSequence> SIMPLE_STRING_REPLY_PARSER =
      new SimpleStringReplyParser<>(CHAR_SEQUENCE_PARSER);

  public static IntegerReplyParser<Integer> integerReply() {
    return INTEGER_REPLY_PARSER;
  }

  public static IntegerReplyParser<Long> longReply() {
    return LONG_REPLY_PARSER;
  }

  public static SimpleStringReplyParser<CharSequence> simpleStringReply() {
    return SIMPLE_STRING_REPLY_PARSER;
  }

  public static <T> BulkStringReplyParser<T> bulkStringReply(BulkStringBuilderFactory<?, ? extends T> builderFactory) {
    return new BulkStringReplyParser<>(len -> new BulkStringParser<>(len, builderFactory));
  }

  public static <E, T> ArrayReplyParser<T> arrayReply(ArrayBuilderFactory<E, ? extends T> arrayBuilderFactory,
      BulkStringBuilderFactory<?, ? extends E> elementBuilderFactory) {
    ReplyParser<E> elementParser = bulkStringReply(elementBuilderFactory);
    return new ArrayReplyParser<>(len -> new ArrayParser<>(len, arrayBuilderFactory, elementParser));
  }

  public static <K, V, T> ArrayReplyParser<T> mapReply(MapBuilderFactory<K, V, ? extends T> arrayBuilderFactory,
      BulkStringBuilderFactory<?, ? extends K> keyBuilderFactory,
      BulkStringBuilderFactory<?, ? extends V> valueBuilderFactory) {
    CombiningReplyParser<K, V> kvParser =
        CombiningReplyParser.combine(bulkStringReply(keyBuilderFactory), bulkStringReply(valueBuilderFactory));
    return new ArrayReplyParser<>(len -> new ArrayAsMapParser<>(len / 2, arrayBuilderFactory, kvParser));
  }

  public static <E, T> ScanReplyParser<T> scanReply(ArrayBuilderFactory<E, ? extends T> arrayBuilderFactory,
      BulkStringBuilderFactory<?, ? extends E> elementBuilderFactory) {
    ReplyParser<E> elementParser = bulkStringReply(elementBuilderFactory);
    return new ScanReplyParser<>(len -> new ArrayParser<>(len, arrayBuilderFactory, elementParser));
  }
}

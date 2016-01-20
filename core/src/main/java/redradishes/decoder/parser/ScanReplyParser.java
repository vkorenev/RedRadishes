package redradishes.decoder.parser;

import redradishes.ScanResult;
import redradishes.decoder.BulkStringBuilders;

import java.util.function.IntFunction;

import static redradishes.decoder.Replies.bulkStringReply;
import static redradishes.decoder.parser.ErrorParser.errorParser;
import static redradishes.decoder.parser.ReplyParser.combine;

public class ScanReplyParser<T> extends AnyReplyParser<ScanResult<T>> {
  private static final UnexpectedReplyTypeParsers UNEXPECTED = new UnexpectedReplyTypeParsers("array");
  private static final Parser<Void> L_2_PARSER = new ExpectedResultParser<>(new byte[]{'2', '\r', '\n'}, null);
  private static final ReplyParser<Long> CURSOR_PARSER = bulkStringReply(BulkStringBuilders._long());

  public ScanReplyParser(IntFunction<? extends ReplyParser<T>> elementsParserFactory) {
    super(UNEXPECTED.simpleStringParser(), errorParser(), UNEXPECTED.integerParser(), UNEXPECTED.nilBulkStringParser(),
        scanResultParser(elementsParserFactory));
  }

  private static <T> ReplyParser<ScanResult<T>> scanResultParser(
      IntFunction<? extends ReplyParser<T>> elementsParserFactory) {
    ReplyParser<T> elementsParser = new ArrayReplyParser<>(elementsParserFactory);
    return combine(L_2_PARSER, combine(CURSOR_PARSER, elementsParser, ScanResult::new), (a, b) -> b);
  }
}

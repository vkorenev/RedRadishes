package redradishes.decoder.parser;

import redradishes.ScanResult;
import redradishes.decoder.BulkStringBuilders;

import java.util.function.IntFunction;

import static redradishes.decoder.parser.ErrorParser.errorParser;
import static redradishes.decoder.parser.ExpectedResultParser.nilParser;
import static redradishes.decoder.parser.ReplyParser.combine;

public class ScanReplyParser<T> extends AnyReplyParser<ScanResult<T>> {
  private static final Parser<Void> L_2_PARSER = new ExpectedResultParser<>(new byte[]{'2', '\r', '\n'}, null);
  private static final ReplyParser<Long> CURSOR_PARSER = RespParsers.bulkStringParser(BulkStringBuilders._long());

  public ScanReplyParser(IntFunction<Parser<T>> elementsParserFactory) {
    super(new UnexpectedSimpleReplyParser<>("simple string"), errorParser(),
        new UnexpectedSimpleReplyParser<>("integer"), nilParser(), scanResultParser(elementsParserFactory));
  }

  private static <T> ReplyParser<ScanResult<T>> scanResultParser(IntFunction<Parser<T>> elementsParserFactory) {
    ReplyParser<T> elementsParser = RespParsers.arrayReplyParser(elementsParserFactory);
    return combine(L_2_PARSER, combine(CURSOR_PARSER, elementsParser, ScanResult::new), (a, b) -> b);
  }
}

package redradishes.decoder.parser;

import redradishes.ScanResult;
import redradishes.decoder.BulkStringBuilders;

import java.util.function.IntFunction;

import static redradishes.decoder.parser.ExpectedResultParser.nilParser;
import static redradishes.decoder.parser.SeqParser.seq;

public class ScanReplyParser<T> extends AnyReplyParser<ScanResult<T>> {
  private static final Parser<Void> L_2_PARSER = new ExpectedResultParser<>(new byte[]{'2', '\r', '\n'}, null);
  private static final Parser<Long> CURSOR_PARSER = RespParsers.bulkStringParser(BulkStringBuilders._long());

  public ScanReplyParser(IntFunction<Parser<T>> elementsParserFactory) {
    super(new UnexpectedSimpleReplyParser<>("simple string"), new ErrorParser<>(),
        new UnexpectedSimpleReplyParser<>("integer"), nilParser(), scanResultParser(elementsParserFactory));
  }

  private static <T> Parser<ScanResult<T>> scanResultParser(IntFunction<Parser<T>> elementsParserFactory) {
    Parser<T> elementsParser = RespParsers.arrayReplyParser(elementsParserFactory);
    return seq(L_2_PARSER, seq(CURSOR_PARSER, elementsParser).mapToParser(ScanResult::new)).mapToParser((a, b) -> b);
  }
}

package redradishes.decoder.parser;

public class BulkStringReplyParser<T> extends SuccessOrFailureParser<T> {
  public BulkStringReplyParser(Parser<T> parser) {
    super('$', parser);
  }
}

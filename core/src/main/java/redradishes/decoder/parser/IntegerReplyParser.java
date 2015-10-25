package redradishes.decoder.parser;

public class IntegerReplyParser<T> extends SuccessOrFailureParser<T> {
  public IntegerReplyParser(Parser<T> parser) {
    super(':', parser);
  }
}
